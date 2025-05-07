package org.example.server.processor;

import org.example.server.ChannelContextHolder;
import org.example.server.ConnectionStatusManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class SimpleHttpRequestProcessor implements Runnable {
    private final SocketChannel connection;
    private final SelectionKey selectionKey;

    public SimpleHttpRequestProcessor(SocketChannel connection, SelectionKey selectionKey) {
        this.connection = connection;
        this.selectionKey = selectionKey;
    }

    @Override
    public void run() {
        try {
            String httpResponse = makeTmpResponse();
            ByteBuffer responseByteBuffer = ByteBuffer.wrap(httpResponse.getBytes(StandardCharsets.UTF_8));
            this.connection.write(responseByteBuffer);
            if(responseByteBuffer.hasRemaining()) {
                ChannelContextHolder.holdPendingOutput(this.connection, responseByteBuffer);
                selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
            } else {
                ConnectionStatusManager.keepIdleConnectionAlive(this.connection, System.currentTimeMillis());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String makeTmpResponse() {
        return "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: 95\r\n" +
                "\r\n" +
                "Hello World Hello World Hello World Hello World Hello World Hello World Hello World Hello World";
    }
}
