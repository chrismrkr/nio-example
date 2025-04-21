package org.example.server;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NioIdleConnectionManager {
    private static Map<SocketChannel, IdleConnectionDetails> keepAliveMap = new ConcurrentHashMap<>();

    public static boolean isTimeout(SocketChannel connection) {
        if(!keepAliveMap.containsKey(connection)) {
            throw new IllegalStateException("Socket Channel Not Connected");
        }
        IdleConnectionDetails idleConnectionDetails = keepAliveMap.get(connection);
        long currentTimeMillis = System.currentTimeMillis();
        return currentTimeMillis - idleConnectionDetails.getLastAccessTime() > idleConnectionDetails.getLastAccessTime();
    }
    public static boolean isConnected(SocketChannel connection) {
        return keepAliveMap.containsKey(connection);
    }
    public static void close(SocketChannel connection) {
        keepAliveMap.remove(connection);
    }
    public static void setIdleConnection(SocketChannel connection, long lastAccessTime, Long timeOut) {
        keepAliveMap.put(connection, new IdleConnectionDetails(lastAccessTime, timeOut));
    }

    public static void setIdleConnection(SocketChannel connection, long lastAccessTime) {
        keepAliveMap.put(connection, new IdleConnectionDetails(lastAccessTime, 3000L));
    }

    private static class IdleConnectionDetails {
        private Long lastAccessTime;
        private Long timeOutMs;
        public IdleConnectionDetails(Long lastAccessTime, Long timeOutMs) {
            this.lastAccessTime = lastAccessTime;
            this.timeOutMs = timeOutMs;
        }
        public Long getLastAccessTime() {
            return lastAccessTime;
        }
        public Long getTimeOut() {
            return timeOutMs;
        }
    }

}
