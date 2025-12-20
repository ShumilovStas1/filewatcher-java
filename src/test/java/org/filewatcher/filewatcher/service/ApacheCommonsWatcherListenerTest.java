package org.filewatcher.filewatcher.service;

import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.filewatcher.filewatcher.FileEvent;
import org.filewatcher.filewatcher.Type;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApacheCommonsWatcherListenerTest {
    CollectingFileEventListener listener = new CollectingFileEventListener();
    FileAlterationMonitor monitor;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        monitor = new FileAlterationMonitor(10);
        FileAlterationObserver observer = new FileAlterationObserver(tempDir.toFile());
        observer.addListener(new ApacheCommonsWatcherListener(listener));
        monitor.addObserver(observer);
    }

    @AfterEach
    void tearDown() throws Exception {
        monitor.stop();
    }

    @Test
    void testFileCreate() throws Exception {
        monitor.start();
        Files.createFile(tempDir.resolve("testFile"));
        List<FileEvent> events = listener.awaitEvents(1);
        assertThat(events).containsExactly(new FileEvent(Type.CREATED, Path.of("testFile"), false, tempDir));
    }

    @Test
    void testDirCreate() throws Exception {
        monitor.start();
        Files.createDirectory(tempDir.resolve("testFile"));
        List<FileEvent> events = listener.awaitEvents(1);
        assertThat(events).containsExactly(new FileEvent(Type.CREATED, Path.of("testFile"), true, tempDir));
    }

    @Test
    void testFileDelete() throws Exception {
        Path testFile = Files.createFile(tempDir.resolve("testFile"));
        monitor.start();
        Files.delete(testFile);
        List<FileEvent> events = listener.awaitEvents(1);
        assertThat(events).containsExactly(new FileEvent(Type.DELETED, Path.of("testFile"), false, tempDir));
    }

    @Test
    void testDirDelete() throws Exception {
        Path testDir = Files.createDirectory(tempDir.resolve("testDir"));
        monitor.start();
        Files.delete(testDir);
        List<FileEvent> events = listener.awaitEvents(1);
        assertThat(events).containsExactly(new FileEvent(Type.DELETED, Path.of("testDir"), true, tempDir));
    }

    @Test
    void testFileModify() throws Exception {
        Path testFile = Files.createFile(tempDir.resolve("testFile"));
        monitor.start();
        Files.write(testFile, "m".getBytes());
        List<FileEvent> events = listener.awaitEvents(1);
        assertThat(events).contains(new FileEvent(Type.MODIFIED, Path.of("testFile"), false, tempDir));
    }

    @Test
    void testMoveFile() throws Exception {
        Path testDir = Files.createDirectory(tempDir.resolve("testDir1"));
        Path anotherDir = Files.createDirectory(tempDir.resolve("testDir2"));
        Path testFile = Files.createFile(testDir.resolve("testFile"));
        monitor.start();
        Files.move(testFile, anotherDir.resolve("testFile"), StandardCopyOption.ATOMIC_MOVE);
        List<FileEvent> events = listener.awaitEvents(2);
        assertThat(events).containsOnly(
                new FileEvent(Type.DELETED, Path.of("testDir1/testFile"), false, tempDir),
                new FileEvent(Type.CREATED, Path.of("testDir2/testFile"), false, tempDir)
        );
    }

    @Test
    void testMoveDir() throws Exception {
        Path testDir = Files.createDirectory(tempDir.resolve("testDir1"));
        Files.createFile(testDir.resolve("testFile"));
        Path anotherDir = Files.createDirectory(tempDir.resolve("testDir2"));
        monitor.start();
        Files.move(testDir, anotherDir.resolve("movedDir"), StandardCopyOption.ATOMIC_MOVE);
        List<FileEvent> events = listener.awaitEvents(2);
        assertThat(events).containsOnly(
                new FileEvent(Type.DELETED, Path.of("testDir1/testFile"), false, tempDir),
                new FileEvent(Type.DELETED, Path.of("testDir1"), true, tempDir),
                new FileEvent(Type.CREATED, Path.of("testDir2/movedDir"), true, tempDir),
                new FileEvent(Type.CREATED, Path.of("testDir2/movedDir/testFile"), false, tempDir)
        );
    }
}