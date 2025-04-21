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
    private static Map<SocketChannel, HttpRequestStreamHolder> streamHolder = new HashMap<>();
    private static Map<SocketChannel, ByteBuffer> pendingWriteMap = new HashMap<>();
    private static Map<SocketChannel, Long> TTL = new HashMap<>();
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
                        consoleLogger.info("ACCEPT END -> REGISTER READ EVNET");

                    } else if(selectionKey.isReadable()) {
                        SocketChannel client = (SocketChannel) selectionKey.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int byteReads = client.read(buffer);

                        if(!streamHolder.containsKey(client)) {
                            streamHolder.put(client, new HttpRequestStreamHolder());
                        }

                        if(byteReads == -1) { // 연결 종료 FIN 수신
                            client.close();
                            streamHolder.remove(client);
                            continue;
                        } else if(byteReads == 0) {
                            continue;
                        }


                        streamHolder.get(client).write(buffer);
                        if(streamHolder.get(client).isDone()) {
                            HttpRequest httpRequest = streamHolder.get(client).build();
                            streamHolder.remove(client);

                            consoleLogger.info("READ END -> DO WRITE OR REGISTER WRITE EVENT");
                            executeThreadPool.submit(() -> {
                                System.out.println("DO SOMETHING!");
                            });

                            String httpResponse = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: text/plain\r\n" +
                                    "Content-Length: 11\r\n" +
                                    "\r\n" +
                                    "Hello World";
                            ByteBuffer responseBuffer = ByteBuffer.wrap(httpResponse.getBytes(StandardCharsets.UTF_8));
                            int write = client.write(responseBuffer);
                            if(responseBuffer.hasRemaining()) {
                                pendingWriteMap.put(client, responseBuffer);
                                selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
                            } else {
                                if(httpRequest.getHeaders().containsKey("Connection") && httpRequest.getHeaders().get("Connection").equals("Keep-Alive")) {
                                    // TODO. Keep-Alive 설정
                                }
                                else {
                                    client.close();
                                }
                            }
                        }
                    } else if(selectionKey.isWritable()) {
                        SocketChannel client = (SocketChannel) selectionKey.channel();
                        ByteBuffer buffer = pendingWriteMap.get(client);
                        if (buffer != null) {
                            client.write(buffer);
                            if (buffer.hasRemaining()) {
                                pendingWriteMap.put(client, buffer);
                            } else {
                                selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE); // WRITE 감시 해제
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