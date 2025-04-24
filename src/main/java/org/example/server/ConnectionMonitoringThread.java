package org.example.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class ConnectionMonitoringThread {
    public static void start(Selector selector) throws IOException, InterruptedException {
        while(true) {
            Set<SelectionKey> keys = selector.keys();
            for(SelectionKey key : keys) {
                if(!(key.channel() instanceof SocketChannel)) {
                    continue;
                }
                SocketChannel connection = (SocketChannel) key.channel();
                boolean isOpened = connection.isConnected();
                if(ConnectionStatusManager.isOccupied(connection)) {
                    continue;
                }
                if(ConnectionStatusManager.isTimeout(connection)) {
                    connection.close();
                    key.cancel();
                }
            }
            Thread.sleep(500);
        }
    }

}
