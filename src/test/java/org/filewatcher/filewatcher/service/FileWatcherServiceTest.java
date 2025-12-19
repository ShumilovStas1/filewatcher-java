package org.filewatcher.filewatcher.service;

import org.filewatcher.filewatcher.FileEvent;
import org.filewatcher.filewatcher.Type;
import org.filewatcher.filewatcher.ws.FileEventListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class FileWatcherServiceTest {
    static ExecutorService executorService ;
    @BeforeAll
    static void setup() {
        executorService = Executors.newSingleThreadExecutor();
    }

    @AfterAll
    static void cleanup() {
        executorService.shutdownNow();
    }

    @TempDir
    Path tempDir;
    @TempDir
    Path anotherTempDir;
    DirsToWatch dirsToWatch;
    CollectingFileEventListener listener;
    FileWatcherService fileWatcherService;

    @BeforeEach
    void setUp() {
        dirsToWatch = new DirsToWatch(List.of(tempDir, anotherTempDir));
        listener = new CollectingFileEventListener();
        fileWatcherService = new FileWatcherService(dirsToWatch, executorService, listener);

    }

    @AfterEach
    void tearDown() {
        fileWatcherService.stop();
    }

    @Test
    void testFileCreate() throws IOException {
        fileWatcherService.start();
        Files.createFile(tempDir.resolve("testFile"));
        List<FileEvent> events = awaitEvents(1);
        assertThat(events).containsExactly(new FileEvent(Type.CREATED, Path.of("testFile"), false, tempDir));
    }

    @Test
    void testDirCreate() throws IOException {
        fileWatcherService.start();
        Files.createDirectory(tempDir.resolve("testFile"));
        List<FileEvent> events = awaitEvents(1);
        assertThat(events).containsExactly(new FileEvent(Type.CREATED, Path.of("testFile"), true, tempDir));
    }

    @Test
    void testFileDelete() throws IOException {
        Path testFile = Files.createFile(tempDir.resolve("testFile"));
        fileWatcherService.start();
        Files.delete(testFile);
        List<FileEvent> events = awaitEvents(1);
        assertThat(events).containsExactly(new FileEvent(Type.DELETED, Path.of("testFile"), false, tempDir));
    }

    @Test
    void testDirDelete() throws IOException {
        Path testDir = Files.createDirectory(tempDir.resolve("testDir"));
        fileWatcherService.start();
        Files.delete(testDir);
        List<FileEvent> events = awaitEvents(1);
        assertThat(events).containsExactly(new FileEvent(Type.DELETED, Path.of("testDir"), true, tempDir));
    }

    @Test
    void testFileModify() throws IOException {
        Path testFile = Files.createFile(tempDir.resolve("testFile"));
        fileWatcherService.start();
        Files.write(testFile, "m".getBytes());
        List<FileEvent> events = awaitEvents(1);
        assertThat(events).contains(new FileEvent(Type.MODIFIED, Path.of("testFile"), false, tempDir));
    }

    @Test
    void testDirModify() throws IOException {
        Path testDir = Files.createDirectory(tempDir.resolve("testDir"));
        fileWatcherService.start();
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-----");
        Files.setPosixFilePermissions(testDir, perms);
        List<FileEvent> events = awaitEvents(1);
        assertThat(events).contains(new FileEvent(Type.MODIFIED, Path.of("testDir"), true, tempDir));
    }

    @Test
    void testMoveFile() throws IOException {
        Path testDir = Files.createDirectory(tempDir.resolve("testDir1"));
        Path anotherDir = Files.createDirectory(tempDir.resolve("testDir2"));
        Path testFile = Files.createFile(testDir.resolve("testFile"));
        fileWatcherService.start();
        Files.move(testFile, anotherDir.resolve("testFile"), StandardCopyOption.ATOMIC_MOVE);
        List<FileEvent> events = awaitEvents(2);
        assertThat(events).containsOnly(
                new FileEvent(Type.DELETED, Path.of("testDir1/testFile"), false, tempDir),
                new FileEvent(Type.CREATED, Path.of("testDir2/testFile"), false, tempDir)
        );
    }

    @Test
    void testMoveDir() throws IOException {
        Path testDir = Files.createDirectory(tempDir.resolve("testDir1"));
        Files.createFile(testDir.resolve("testFile"));
        Path anotherDir = Files.createDirectory(tempDir.resolve("testDir2"));
        fileWatcherService.start();
        Files.move(testDir, anotherDir.resolve("movedDir"), StandardCopyOption.ATOMIC_MOVE);
        List<FileEvent> events = awaitEvents(2);
        assertThat(events).containsOnly(
                new FileEvent(Type.DELETED, Path.of("testDir1"), true, tempDir),
                new FileEvent(Type.CREATED, Path.of("testDir2/movedDir"), true, tempDir)
        );
    }

    private List<FileEvent> awaitEvents(int expectedCount) {
        int attempts = 0;
        List<FileEvent> events = listener.getEvents();
        while (events.size() < expectedCount && attempts < 10) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            events = listener.getEvents();
            attempts++;
        }
        if (events.size() < expectedCount) {
            throw new AssertionError("Expected " + expectedCount + " events but got " + events.size());
        }
        return events;
    }

    private static class CollectingFileEventListener implements FileEventListener {
        List<FileEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public void onFileEvent(FileEvent event) {
            events.add(event);
        }

        public List<FileEvent> getEvents() {
            return events;
        }
    }
}