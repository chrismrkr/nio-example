package org.example.server;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

public class PollerThread {
    public static void start(Selector selector) throws IOException, InterruptedException {
        while(true) {
            for(SelectionKey key : selector.keys()) {
                SocketChannel connection = (SocketChannel) key.channel();
//                if(NioIdleConnectionManager.isUsed()) continue;
                if(NioIdleConnectionManager.isTimeout(connection)) {
                    connection.close();
                    selector.wakeup();
                }
            }
            Thread.sleep(500);
        }
    }

}
