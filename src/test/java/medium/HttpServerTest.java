package medium;

import org.example.server.HttpServer;
import org.example.server.config.ServerConfig;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class HttpServerTest {
    private static Thread serverThread;

    @BeforeClass
    public static void setup() {
        new Thread(() -> {
            try {
                HttpServer.main(null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

    }
    @Test
    public void 테스트() throws IOException, InterruptedException {
        // given
        try(Socket socket = new Socket("localhost", ServerConfig.getInstance().getPort())) {
            OutputStream out = socket.getOutputStream();
            out.write("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            Assert.assertTrue(response.toString().contains("Hello"));
        }
    }
}
