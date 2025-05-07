package org.example.server;


import org.example.server.config.ServerConfig;
import org.example.server.processor.SimpleHttpRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;


public class HttpServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
    private static final java.util.logging.Logger consoleLogger = java.util.logging.Logger.getLogger(HttpServer.class.getCanonicalName());
    public HttpServer() throws IOException {}
    public void start() throws IOException {
        ServerConfig serverConfig = ServerConfig.getInstance();
        ExecutorService executeThreadPool = Executors.newFixedThreadPool(serverConfig.getThreadPoolSize());

        try(ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()
                .bind(new InetSocketAddress("localhost", serverConfig.getPort()), 100);

            Selector selector = Selector.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            consoleLogger.info("NIO HTTP SERVER STARTED");

            while(true) {
                int select = selector.select(100);
                if(select == 0) {
                    clearTimeoutConnection();
                    continue;
                }

                Iterator<SelectionKey> currentSelectionEvents = selector.selectedKeys().iterator();

                while(currentSelectionEvents.hasNext()) {
                    SelectionKey selectionKey = currentSelectionEvents.next();

                    if(selectionKey.isAcceptable()) {
                        acceptConnection(serverSocketChannel, selector, currentSelectionEvents);
                        continue;
                    }

                    SocketChannel client = (SocketChannel) selectionKey.channel();
                    if(selectionKey.isReadable()) {
                        int readBufferCapacity = 32;
                        ByteBuffer buffer = ByteBuffer.allocate(readBufferCapacity);
                        int byteReads = client.read(buffer);

                        if(byteReads == ChannelContextHolder.FIN_ACK) {
                            // 연결 종료 FIN 수신
                            ChannelContextHolder.freeInputHolder(client);
                            client.close();
                            continue;
                        } else if(byteReads == 0) {
                            continue;
                        }

                        if(ChannelContextHolder.isNewChannel(client)) {
                            ChannelContextHolder.allocateInputHolder(client);
                        }
                        ChannelContextHolder.getInputHolder(client).write(buffer);
                        if(ChannelContextHolder.getInputHolder(client).isReadDone()) {
                            executeThreadPool.submit(new SimpleHttpRequestProcessor(client, selectionKey));
                        }
                    }

                    if(selectionKey.isWritable()) {
                        ByteBuffer buffer = ChannelContextHolder.retrievePendingOutput(client);
                        if (buffer != null) {
                            client.write(buffer);
                            if (buffer.hasRemaining()) {
                                ChannelContextHolder.holdPendingOutput(client, buffer);
                            } else {
                                selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE); // WRITE 감시 해제
                                ChannelContextHolder.releasePendingOutput(client);
                                ConnectionStatusManager.keepIdleConnectionAlive(client, System.currentTimeMillis());
                            }
                        }
                    }

                    currentSelectionEvents.remove();
                }

                clearTimeoutConnection();
            }

        } catch (IOException e) {
            logger.warn("Error accepting connection", e);
            consoleLogger.info(e.getMessage());
        } catch (Exception e) {
            logger.error("Unknown Exception", e);
            consoleLogger.info(e.getMessage());
        }
    }

    private void clearTimeoutConnection() throws IOException {
        Map<SocketChannel, ConnectionStatusManager.IdleConnectionDetails> keepAliveMap = ConnectionStatusManager.getKeepAliveMap();
        for(SocketChannel connection : keepAliveMap.keySet()) {
            if(ConnectionStatusManager.isTimeout(connection)) {
                ConnectionStatusManager.close(connection);
                connection.close();
                consoleLogger.info(connection.toString() + " : Connection Time out");
            }
        }
    }

    private void acceptConnection(ServerSocketChannel server, Selector selector, Iterator<SelectionKey> currentSelectionEvents) throws IOException {
        SocketChannel connection = server.accept();
        connection.configureBlocking(false);
        connection.register(selector, SelectionKey.OP_READ);
        currentSelectionEvents.remove();
    }

    public static void main(String[] args) throws IOException {
        try {
            String configFilePath = args[0];
            ServerConfig.createInstance(configFilePath);
        } catch (Exception e) {
            ServerConfig.createInstance("config.json");
        }

        try {
            HttpServer webserver = new HttpServer();
            webserver.start();
        } catch (IOException ex) {
            logger.error("Server could not start", ex);
        }
    }
}