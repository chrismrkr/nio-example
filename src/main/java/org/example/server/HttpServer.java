package org.example.server;


import org.example.http.HttpRequest;
import org.example.http.HttpRequestStreamHolder;
import org.example.server.config.ServerConfig;
import org.example.server.processor.HttpRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class HttpServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
    private static final java.util.logging.Logger consoleLogger = java.util.logging.Logger.getLogger(HttpServer.class.getCanonicalName());
    public HttpServer() throws IOException {}
    public void start() throws IOException {
        ServerConfig serverConfig = ServerConfig.getInstance();
        ExecutorService executeThreadPool = Executors.newFixedThreadPool(serverConfig.getThreadPoolSize());

        try(ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()
                .bind(new InetSocketAddress("localhost", serverConfig.getPort()), 100);
            Selector selector = Selector.open();) {

            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while(selector.select() > 0) {
                Iterator<SelectionKey> currentSelectionEvents = selector.selectedKeys().iterator();

                while(currentSelectionEvents.hasNext()) {
                    SelectionKey selectionKey = currentSelectionEvents.next();

                    if(selectionKey.isAcceptable()) {
                        SocketChannel client = serverSocketChannel.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                        consoleLogger.info(client.toString() +": Client is Accepted.");
                        continue;
                    }

                    // TODO. AcceptorThread와 PollerThread를 분리한다.
                    // 각 스레드 모두 독립적인 selector를 갖는다.
                    // AcceptorThread는 연결을 accept하면 PollerThread의 selector를 wakeup한 후, channel을 등록한다.
                    // PollerThread는 read, write 이벤트를 처리하고 응답한다.
                    // 또한, PollerThread는 selector.select(1000)로 주기적으로 selector를 깨워서 Keep-Alive를 확인한다.


                    if(selectionKey.isReadable()) {
                        SocketChannel client = (SocketChannel) selectionKey.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int byteReads = client.read(buffer);

                        if(byteReads == ChannelContextHolder.FIN_ACK) { // 연결 종료 FIN 수신
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
                        consoleLogger.info(client.toString()+ ": Client Reads Input");
                        if(ChannelContextHolder.getInputHolder(client).isDone()) {
                            consoleLogger.info(client.toString() + ": Client Read All Input");
                            HttpRequest httpRequest = ChannelContextHolder.build(client);

                            executeThreadPool.submit(() -> {
                                consoleLogger.info(client.toString() + ": HELLO WORLD");
                            });

                            String httpResponse = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: text/plain\r\n" +
                                    "Content-Length: 11\r\n" +
                                    "\r\n" +
                                    "Hello World";


                            ByteBuffer responseBuffer = ByteBuffer.wrap(httpResponse.getBytes(StandardCharsets.UTF_8));
                            client.write(responseBuffer);
                            if(responseBuffer.hasRemaining()) {
                                ChannelContextHolder.holdPendingOutput(client, buffer);
                                selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                                consoleLogger.info(client.toString() + ": Client Should Write More");
                            } else {
                                consoleLogger.info(client.toString() + ": Client Wrote All Output");
                                if(httpRequest.getHeaders().containsKey("Connection") && httpRequest.getHeaders().get("Connection").equals("Keep-Alive")) {
                                    // TODO. Keep-Alive 설정
                                    client.close();
                                }
                                else {
                                    client.close();
                                }
                            }
                        } else {
                            consoleLogger.info(client.toString()+ ": Client Should Read Input More");
                        }
                    }

                    if(selectionKey.isWritable()) {
                        SocketChannel client = (SocketChannel) selectionKey.channel();
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
                            }
                        }
                    }
                    currentSelectionEvents.remove();
                }
            }

        } catch (IOException e) {
            logger.warn("Error accepting connection", e);
        } catch (Exception e) {
            logger.error("Unknown Exception", e);
        }
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