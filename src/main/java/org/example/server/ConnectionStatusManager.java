package org.example.server;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ConnectionStatusManager {
    private static final Long CONNECTION_TIME_OUT_MS = 30000L;
    private static Map<SocketChannel, IdleConnectionDetails> keepAliveMap = new ConcurrentHashMap<>();
    private static final Logger consoleLogger = Logger.getLogger(ConnectionStatusManager.class.getCanonicalName());

    public static boolean isTimeout(SocketChannel connection) {
        if(!keepAliveMap.containsKey(connection)) {
            throw new IllegalStateException("Socket Channel Not Connected");
        }
        IdleConnectionDetails idleConnectionDetails = keepAliveMap.get(connection);
        long currentTimeMillis = System.currentTimeMillis();
        if(currentTimeMillis - idleConnectionDetails.getLastAccessTime() > idleConnectionDetails.getTimeOut()) {
            consoleLogger.info(connection.toString() + ": Client Channel Time out");
            return true;
        } else return false;
    }
    public static boolean isConnected(SocketChannel connection) {
        return keepAliveMap.containsKey(connection);
    }
    public static void close(SocketChannel connection) {
        consoleLogger.info(connection.toString() + ": Close Socket Channel Connection");
        keepAliveMap.remove(connection);
    }
    public static void setIdleConnection(SocketChannel connection, long lastAccessTime, Long timeOut) {
        consoleLogger.info(connection.toString() + ": Keep Socket Channel Alive");
        keepAliveMap.put(connection, new IdleConnectionDetails(lastAccessTime, timeOut));
    }
    public static void setIdleConnection(SocketChannel connection, long lastAccessTime) {
        consoleLogger.info(connection.toString() + ": Keep Socket Channel Alive");
        keepAliveMap.put(connection, new IdleConnectionDetails(lastAccessTime, 3000L));
    }

    public static Map<SocketChannel, IdleConnectionDetails> getKeepAliveMap() {
        return keepAliveMap;
    }

    static class IdleConnectionDetails {
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
