package org.example.server;

import org.example.http.HttpRequest;
import org.example.http.HttpRequestStreamHolder;

import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class ChannelContextHolder {
    private static Map<SocketChannel, HttpRequestStreamHolder> streamHolderMap = new HashMap<>();
    private static Map<SocketChannel, ByteBuffer> pendingWriteMap = new HashMap<>();
    private static Map<SocketChannel, Long> TTL = new HashMap<>();
    public static final int FIN_ACK = -1;

    public static boolean isNewChannel(SocketChannel socketChannel) {
        return !streamHolderMap.containsKey(socketChannel);
    }

    public static void allocateInputHolder(SocketChannel socketChannel) {
        streamHolderMap.put(socketChannel, new HttpRequestStreamHolder());
    }

    public static void freeInputHolder(SocketChannel socketChannel) {
        streamHolderMap.remove(socketChannel);
    }

    public static HttpRequestStreamHolder getInputHolder(SocketChannel socketChannel) {
        return streamHolderMap.get(socketChannel);
    }

    public static HttpRequest build(SocketChannel socketChannel) {
        HttpRequest build = streamHolderMap.get(socketChannel).build();
        streamHolderMap.remove(socketChannel);
        return build;
    }

    public static void holdPendingOutput(SocketChannel socketChannel, ByteBuffer byteBuffer) {
        pendingWriteMap.put(socketChannel, byteBuffer);
    }

    public static ByteBuffer retrievePendingOutput(SocketChannel socketChannel) {
        return pendingWriteMap.get(socketChannel);
    }
    public static void releasePendingOutput(SocketChannel socketChannel) {
        pendingWriteMap.remove(socketChannel);
    }
}
