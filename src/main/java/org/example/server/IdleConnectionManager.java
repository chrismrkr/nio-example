package org.example.server;

import org.example.server.processor.HttpRequestProcessor;
import org.example.server.processor.RequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IdleConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(IdleConnectionManager.class);
    private static final java.util.logging.Logger consoleLogger = java.util.logging.Logger.getLogger(IdleConnectionManager.class.getCanonicalName());
    private static Map<Socket, Long> keepAliveConnections = new ConcurrentHashMap<>();
    private static int BACKGROUND_THREAD_NUM = 3;
    private static ExecutorService backgroundThreadPool = Executors.newFixedThreadPool(BACKGROUND_THREAD_NUM);
    private static long TIME_OUT_MS = 2000L;
    public static void monitor(ExecutorService mainThreadPool) throws IOException {
        Runnable backGroundTask = () -> {
            consoleLogger.info("Idle Connection Manager Starts");
            while(true) {
                try {
                    doMonitor(mainThreadPool);
                } catch (IOException e) {
                    logger.warn("Exception Occurred While Monitoring Idle Connection", e);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    logger.warn("Idle Connection Monitoring Thread Died");
                    break;
                }
            }
        };

        for(int i=0; i<BACKGROUND_THREAD_NUM; i++) {
            backgroundThreadPool.execute(backGroundTask);
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static boolean isTimeout(long createdTime) {
        return System.currentTimeMillis() - createdTime > TIME_OUT_MS;
    }

    public static void reserveConnection(Socket connection) {
        keepAliveConnections.put(connection, System.currentTimeMillis());
    }

    private static void doMonitor(ExecutorService executorService) throws IOException {
        for (Map.Entry<Socket, Long> entry : keepAliveConnections.entrySet()) {
            Socket connection = entry.getKey();
            long createdTime = entry.getValue();

            InputStream input = connection.getInputStream();
            if (input.available() > 0) {
                executorService.submit(new HttpRequestProcessor(connection));
                keepAliveConnections.remove(connection);
                consoleLogger.info("Idle Connection reused: Connection " + createdTime);
            }
            if (isTimeout(createdTime)) {
                connection.close();
                keepAliveConnections.remove(connection);
                consoleLogger.info("Idle Connection Close: Connection " + createdTime);
            }
        }
    }
}
