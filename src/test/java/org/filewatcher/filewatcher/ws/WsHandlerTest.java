package org.filewatcher.filewatcher.ws;

import com.corundumstudio.socketio.SocketIOServer;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.assertj.core.api.Assertions;
import org.filewatcher.filewatcher.FileEvent;
import org.filewatcher.filewatcher.Type;
import org.filewatcher.filewatcher.service.DirsToWatch;
import org.filewatcher.filewatcher.service.NioFileWatcherService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.mockito.Mockito.when;

@SpringBootTest
class WsHandlerTest {
    @Autowired
    SocketIOServer server;
    @Autowired
    WsHandler wsHandler;
    @MockitoBean
    DirsToWatch dirsToWatch;
    @MockitoBean
    NioFileWatcherService fileWatcherService;

    Socket client;
    volatile List<String> directories = List.of();
    final List<FileEvent> receivedEvents = new CopyOnWriteArrayList<>();

    @AfterEach
    void tearDown() {
        client.disconnect();
    }

    @BeforeEach
    void setUp() throws URISyntaxException {
        client = IO.socket(new URI("http://localhost:" + server.getConfiguration().getPort()));
        client.on("dirs", args -> {
            if (args[1] instanceof JSONArray array) {
                List<String> dirs = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    dirs.add(array.optString(i));
                }
                directories = dirs;
            }
        });
        client.on("fs_event", args -> {
            if (args[0] instanceof JSONObject obj) {
                FileEvent event = new FileEvent(
                        Type.valueOf(obj.optString("type").toUpperCase()),
                        Path.of(obj.optString("src_path")),
                        obj.optBoolean("is_dir"),
                        Path.of(obj.optString("root_path"))
                );
                receivedEvents.add(event);
            }
        });

    }

    @Test
    void testCreateFileEvent() {
        when(dirsToWatch.getAbsolutePaths()).thenReturn(List.of(Path.of("/watched"), Path.of("/another")));
        awaitConnected(client);
        List<String> dirs = awaitDirectories();
        Assertions.assertThat(dirs).containsOnly("/watched", "/another");
        FileEvent eventToSend = new FileEvent(Type.CREATED, Path.of("test.txt"), false, Path.of("/watched"));
        wsHandler.onFileEvent(eventToSend);
        List<FileEvent> receivedEvents = awaitEvents(1);
        Assertions.assertThat(receivedEvents).containsExactly(eventToSend);
    }

    private List<FileEvent> awaitEvents(int eventCount) {
        int attempts = 0;
        while (receivedEvents.size() != eventCount && attempts < 10) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            attempts++;
        }
        if (directories.size() == eventCount) {
            throw new AssertionError("No directories received in time");
        }
        List<FileEvent> result = List.copyOf(receivedEvents);
        receivedEvents.clear();
        return result;
    }

    private List<String> awaitDirectories() {
        int attempts = 0;
        while (directories.isEmpty() && attempts < 10) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            attempts++;
        }
        if (directories.isEmpty()) {
            throw new AssertionError("No directories received in time");
        }
        return directories;
    }

    private void awaitConnected(Socket socket) {
        socket.connect();
        int attempts = 0;
        while (!socket.connected() && attempts < 10) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            attempts++;
        }
        if (!socket.connected()) {
            throw new AssertionError("Socket did not connect in time");
        }
    }
}