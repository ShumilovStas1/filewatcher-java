package org.filewatcher.filewatcher.ws;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ExceptionListener;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DummyLogExceptionListener implements ExceptionListener {
    private static final Logger log = LoggerFactory.getLogger(DummyLogExceptionListener.class);

    @Override
    public void onEventException(Exception e, List<Object> list, SocketIOClient socketIOClient) {
        log.error("Event exception for client {}", socketIOClient.getSessionId(), e);
    }

    @Override
    public void onDisconnectException(Exception e, SocketIOClient socketIOClient) {
        log.error("Disconnect exception for client {}", socketIOClient.getSessionId(), e);
    }

    @Override
    public void onConnectException(Exception e, SocketIOClient socketIOClient) {
        log.error("Error connecting client {}", socketIOClient.getSessionId(), e);
    }

    @Override
    public void onPingException(Exception e, SocketIOClient socketIOClient) {
        log.error("Ping exception for client {}", socketIOClient.getSessionId(), e);
    }

    @Override
    public void onPongException(Exception e, SocketIOClient socketIOClient) {
        log.error("Pong exception for client {}", socketIOClient.getSessionId(), e);
    }

    @Override
    public boolean exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable) throws Exception {
        log.error("Exception caught in channel {}", channelHandlerContext.channel().id(), throwable);
        return false;
    }

    @Override
    public void onAuthException(Throwable throwable, SocketIOClient socketIOClient) {
        log.error("Auth exception for client {}", socketIOClient.getSessionId(), throwable);
    }
}
