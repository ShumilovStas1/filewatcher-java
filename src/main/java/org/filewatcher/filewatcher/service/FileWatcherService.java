package org.filewatcher.filewatcher.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.filewatcher.filewatcher.FileEvent;
import org.filewatcher.filewatcher.Type;
import org.filewatcher.filewatcher.ws.FileEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

@Service
public class FileWatcherService {
    private static final Logger log = LoggerFactory.getLogger(FileWatcherService.class);

    private final DirsToWatch dirsToWatch;
    private final FileEventListener listener;
    private WatchService ws;
    private final ExecutorService pollEventsExecutor;
    private final Set<Path> watchedDirs = Collections.newSetFromMap(new ConcurrentHashMap<>());
    @Value("${poll.thread.count:1}")
    private int threadCount;

    public FileWatcherService(DirsToWatch dirsToWatch,
                              ExecutorService fileEventPollExecutor,
                              FileEventListener listener) {
        this.dirsToWatch = dirsToWatch;
        this.listener = listener;
        this.pollEventsExecutor = fileEventPollExecutor;
    }

    @PostConstruct
    void start() {
        FileSystem fs = FileSystems.getDefault();
        try {
            ws = fs.newWatchService();
            for (Path toWatch : dirsToWatch.getAbsolutePaths()) {
                registerWatch(toWatch.toAbsolutePath());
            }
            int count = Math.max(1, threadCount);
            IntStream.range(0, count).forEach(i -> {
                log.debug("Submitting pollEvents task to executor {}", i);
                pollEventsExecutor.submit(this::pollEvents);
            });

        } catch (IOException e) {
            throw new RuntimeException("Error starting FileWatcherService", e);
        }
    }

    private void pollEvents() {
        while(!Thread.currentThread().isInterrupted()) {
            try {
                WatchKey key = ws.take();
                for (WatchEvent<?> watchEvent : key.pollEvents()) {
                    Path child = (Path) watchEvent.context();
                    Path root = (Path) key.watchable();
                    var eventType = Type.fromEventKind(watchEvent.kind().name());
                    if (child != null) {
                        Path resolvedSource = root.resolve(child);
                        if (eventType.equals(Type.CREATED) && Files.isDirectory(resolvedSource)) {
                            registerWatch(resolvedSource);
                        }
                        Path resolvedRoot = dirsToWatch.findOriginalRootPath(resolvedSource);
                        Path srcPath = resolvedRoot.relativize(resolvedSource);
                        Path resolvedFilePath = resolvedRoot.resolve(srcPath);
                        FileEvent event = new FileEvent(eventType, srcPath, isDirectory(resolvedFilePath, eventType), resolvedRoot);
                        listener.onFileEvent(event);
                    }
                }
                if (!key.reset()) {
                    log.warn("WatchKey no longer valid: {}", key);
                }
            } catch (InterruptedException | ClosedWatchServiceException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Error polling events", e);
            }
        }
    }

    private boolean isDirectory(Path path, Type eventType) {
        if (Type.DELETED.equals(eventType)) {
            return watchedDirs.remove(path);
        } else {
            return Files.isDirectory(path);
        }
    }


    private void registerWatch(Path newDir) {
        try {
            watchedDirs.add(newDir);
            newDir.register(ws, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            Files.walkFileTree(newDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.error("Unable to register directory {}", file, exc);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!newDir.equals(dir)) {
                        watchedDirs.add(dir);
                        dir.register(ws, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Unable to register directory for watching: {}", newDir, e);
        }
    }

    @PreDestroy
    void stop() {
        log.info("Closing WatchService");
        try {
            ws.close();
        } catch (IOException e) {
            log.error("Error closing WatchService", e);
        }
    }
}
