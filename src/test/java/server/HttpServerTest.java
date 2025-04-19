package server;

import org.example.server.HttpServer;
import org.example.server.config.ServerConfig;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;

public class HttpServerTest {
    private static Thread serverThread;
    private static final Logger logger = Logger.getLogger(String.valueOf(HttpServerTest.class));
    private static final org.slf4j.Logger logWriter = LoggerFactory.getLogger(HttpServerTest.class);

    @BeforeClass
    public static void setUp() {
        serverThread = new Thread(() -> {
            try {
                ServerConfig.createInstance("config.json");
                new HttpServer().start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();

        try {
            // 서버 뜰 시간 약간 줌
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}
    }

    @AfterClass
    public static void tearDown() {
        serverThread.interrupt();
    }

    @Test
    public void localhost_HOST에서_index파일_GET() throws IOException {
        // given
        try (Socket socket = new Socket("localhost", ServerConfig.getInstance().getPort());
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // when
            writer.print("GET /index.html HTTP/1.1\r\n");
            writer.print("Host: localhost\r\n");
            writer.print("\r\n");
            writer.flush();

            // then
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            String result = response.toString();
            assertTrue(result.contains("HTTP/1.1 200 OK"));
            assertTrue(result.contains("Hello localhost")); // index.html 내용 일부
        }
    }

    @Test
    public void 스레드풀_개수만큼_동시_요청_가능() throws InterruptedException {
        // given
        int num = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(num*2);
        CountDownLatch latch = new CountDownLatch(num);
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        // when
        for(int i=0; i<num; i++) {
            executorService.execute(() -> {
                try (Socket socket = new Socket("localhost", ServerConfig.getInstance().getPort());
                     PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
                )
                {

                    // when
                    writer.print("GET /index.html HTTP/1.1\r\n");
                    writer.print("Host: localhost\r\n");
                    writer.print("\r\n");
                    writer.flush();

                    // then
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                    String result = response.toString();
                    results.add(result);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);

        // then
        for(int i=0; i<num; i++) {
            Assert.assertTrue(results.get(i).contains("index.html"));
        }
    }
}
