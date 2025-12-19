package org.filewatcher.filewatcher.ws;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import org.filewatcher.filewatcher.FileEvent;
import org.filewatcher.filewatcher.service.DirsToWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;

@Component
public class WsHandler implements FileEventListener {
    private static final Logger log = LoggerFactory.getLogger(WsHandler.class);

    private final DirsToWatch dirsToWatch;
    private final SocketIOServer server;

    public WsHandler(DirsToWatch dirsToWatch, SocketIOServer server) {
        this.dirsToWatch = dirsToWatch;
        this.server = server;
    }

    @OnConnect
    public void onConnect(SocketIOClient client) {
        log.info("Client connected: {}", client.getSessionId());
        client.sendEvent("dirs", dirsToWatch.getAbsolutePaths().stream().map(Path::toString).toList());
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        log.info("Client disconnected: {}", client.getSessionId());
    }

    @Override
    public void onFileEvent(FileEvent event) {
        try {
            sendEvent(event);
        } catch (Exception e) {
            log.error("Error sending file event to clients: {}", event, e);
        }
    }

    private void sendEvent(FileEvent event) {
        log.info("Detected file event: {}", event);
        Map<String, Object> data = Map.of(
                "is_dir", event.isDir(),
                "type", event.type().name().toLowerCase(),
                "src_path", event.target().toString(),
                "root_path", event.rootDir().toString()
        );
        server.getBroadcastOperations().sendEvent("fs_event", data);
    }
}
