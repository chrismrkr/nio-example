package org.example.server;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionStatusManager {
    private static final Long CONNECTION_TIME_OUT_MS = 30000L;
    private static Map<SocketChannel, IdleConnectionDetails> keepAliveMap = new ConcurrentHashMap<>();
    private static Map<SocketChannel, Long> isOccupied = new ConcurrentHashMap<>();
    private static final java.util.logging.Logger consoleLogger = java.util.logging.Logger.getLogger(ConnectionStatusManager.class.getCanonicalName());
    public static boolean isOccupied(SocketChannel connection) {
        if(!isOccupied.containsKey(connection)) {
            return false;
        }
        if(System.currentTimeMillis() - isOccupied.get(connection) > CONNECTION_TIME_OUT_MS) {
            isOccupied.remove(connection);
            return false;
        }
        consoleLogger.info(connection.toString() + ": Already Occupied");
        return true;
    }

    public static void occupy(SocketChannel connection) {
        consoleLogger.info(connection.toString() + ": get Occupied");
        isOccupied.put(connection, System.currentTimeMillis());
    }
    public static void release(SocketChannel connection) {
        consoleLogger.info(connection.toString() + ": get released");
        isOccupied.remove(connection);
    }
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
