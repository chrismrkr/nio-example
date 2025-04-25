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
            boolean isSelectorOpen = selector.isOpen();
            Set<SelectionKey> keys = selector.keys();
            for(SelectionKey key : keys) {
                boolean valid = key.isValid();

                if(!key.isValid()) continue;
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
                }
            }
            Thread.sleep(500);
        }
    }

}
