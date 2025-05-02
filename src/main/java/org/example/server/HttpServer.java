package org.example.server;


import org.example.http.HttpRequest;
import org.example.server.config.ServerConfig;
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
    public static final BlockingQueue<SocketChannel> timeoutConnectionQueue = new LinkedBlockingQueue<>();
    public HttpServer() throws IOException {}
    public void start() throws IOException {
        ServerConfig serverConfig = ServerConfig.getInstance();
        ExecutorService executeThreadPool = Executors.newFixedThreadPool(serverConfig.getThreadPoolSize());

        try(ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()
                .bind(new InetSocketAddress("localhost", serverConfig.getPort()), 100);

            Selector selector = Selector.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while(true) {
                int select = selector.select(1000);
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
                        ByteBuffer buffer = ByteBuffer.allocate(32);
                        int byteReads = client.read(buffer);

                        if(byteReads == ChannelContextHolder.FIN_ACK) {
                            // 연결 종료 FIN 수신
                            consoleLogger.info(client.toString() + ": Client is Closed.");
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
                        if(ChannelContextHolder.getInputHolder(client).isDone()) {
                            consoleLogger.info(client.toString() + ": Client Read All Input");
                            HttpRequest httpRequest = ChannelContextHolder.build(client);

                            executeThreadPool.submit(() -> {
                                consoleLogger.info(client.toString() + ": HELLO WORLD");
                            });

                            String httpResponse = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: text/plain\r\n" +
                                    "Content-Length: 95\r\n" +
                                    "\r\n" +
                                    "Hello World Hello World Hello World Hello World Hello World Hello World Hello World Hello World";


                            ByteBuffer responseBuffer = ByteBuffer.wrap(httpResponse.getBytes(StandardCharsets.UTF_8));
                            client.write(responseBuffer);
                            if(responseBuffer.hasRemaining()) {
                                ChannelContextHolder.holdPendingOutput(client, buffer);
                                selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                                consoleLogger.info(client.toString() + ": Client Should Write More");
                            } else {
                                consoleLogger.info(client.toString() + ": Client Wrote All Output");
                                ConnectionStatusManager.setIdleConnection(client, System.currentTimeMillis());
                            }
                        } else {
                            consoleLogger.info(client.toString()+ ": Client Should Read Input More");
                        }
                    }

                    if(selectionKey.isWritable()) {
                        ByteBuffer buffer = ChannelContextHolder.retrievePendingOutput(client);
                        if (buffer != null) {
                            client.write(buffer);
                            if (buffer.hasRemaining()) {
                                consoleLogger.info(client.toString() + ": Client Should Write More");
                                ChannelContextHolder.holdPendingOutput(client, buffer);
                            } else {
                                consoleLogger.info(client.toString() + ": Client Wrote All Output");
                                selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE); // WRITE 감시 해제
                                ChannelContextHolder.releasePendingOutput(client);
                                ConnectionStatusManager.setIdleConnection(client, System.currentTimeMillis());
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
                consoleLogger.info(connection.toString() + " : Connection is Closed");
            }
        }
    }

    private void acceptConnection(ServerSocketChannel server, Selector selector, Iterator<SelectionKey> currentSelectionEvents) throws IOException {
        SocketChannel connection = server.accept();
        connection.configureBlocking(false);
        connection.register(selector, SelectionKey.OP_READ);
        currentSelectionEvents.remove();
        consoleLogger.info(connection.toString() + ": IS ACCEPTED");
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