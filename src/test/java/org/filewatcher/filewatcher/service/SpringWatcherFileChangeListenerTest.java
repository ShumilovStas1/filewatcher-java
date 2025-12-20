package org.filewatcher.filewatcher.service;

import org.filewatcher.filewatcher.FileEvent;
import org.filewatcher.filewatcher.Type;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.devtools.filewatch.FileSystemWatcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpringWatcherFileChangeListenerTest {
    CollectingFileEventListener fileEventListener = new CollectingFileEventListener();
    FileSystemWatcher fileWatcherService;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileWatcherService = new FileSystemWatcher(true, Duration.ofMillis(10), Duration.ofMillis(4));
        fileWatcherService.addListener(new SpringWatcherFileChangeListener(fileEventListener));
        fileWatcherService.addSourceDirectory(tempDir.toFile());
    }

    @AfterEach
    void tearDown() {
        fileWatcherService.stop();
    }

    @Test
    void testFileCreate() throws IOException {
        fileWatcherService.start();
        Files.createFile(tempDir.resolve("testFile"));
        List<FileEvent> events = fileEventListener.awaitEvents(1);
        assertThat(events).containsExactly(new FileEvent(Type.CREATED, Path.of("testFile"), false, tempDir));
    }

    @Test
    void testFileDelete() throws IOException {
        Path testFile = Files.createFile(tempDir.resolve("testFile"));
        fileWatcherService.start();
        Files.delete(testFile);
        List<FileEvent> events = fileEventListener.awaitEvents(1);
        assertThat(events).containsExactly(new FileEvent(Type.DELETED, Path.of("testFile"), false, tempDir));
    }

    @Test
    void testFileModify() throws IOException {
        Path testFile = Files.createFile(tempDir.resolve("testFile"));
        fileWatcherService.start();
        Files.write(testFile, "m".getBytes());
        List<FileEvent> events = fileEventListener.awaitEvents(1);
        assertThat(events).contains(new FileEvent(Type.MODIFIED, Path.of("testFile"), false, tempDir));
    }

    @Test
    void testMoveFile() throws IOException {
        Path testDir = Files.createDirectory(tempDir.resolve("testDir1"));
        Path anotherDir = Files.createDirectory(tempDir.resolve("testDir2"));
        Path testFile = Files.createFile(testDir.resolve("testFile"));
        fileWatcherService.start();
        Files.move(testFile, anotherDir.resolve("testFile"), StandardCopyOption.ATOMIC_MOVE);
        List<FileEvent> events = fileEventListener.awaitEvents(2);
        assertThat(events).containsOnly(
                new FileEvent(Type.DELETED, Path.of("testDir1/testFile"), false, tempDir),
                new FileEvent(Type.CREATED, Path.of("testDir2/testFile"), false, tempDir)
        );
    }
}