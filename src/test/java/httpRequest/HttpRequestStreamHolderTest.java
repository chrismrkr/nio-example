package httpRequest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.example.http.HttpRequest;
import org.example.http.HttpRequestStreamHolder;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class HttpRequestStreamHolderTest {

    @Test
    public void 한번에_Stream이_모두_도착() throws IOException {
        // given
        HttpRequestStreamHolder streamHolder = new HttpRequestStreamHolder();
        String rawRequest =
                "POST /food HTTP/1.1\r\n" +
                        "Host: localhost:8080\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: 43\r\n" +
                        "\r\n" +
                        "{ \"foodName\": \"kimchi\", \"price\": 15000 }";
        InputStream inputStream = new ByteArrayInputStream(rawRequest.getBytes(StandardCharsets.UTF_8));
        ByteBuffer wrap = ByteBuffer.wrap(inputStream.readAllBytes());
        // when
        streamHolder.write(wrap);
        HttpRequest httpRequest = streamHolder.build();
        // then
        Assert.assertEquals(httpRequest.getUri(), "/food");
        Assert.assertEquals(httpRequest.getHeaders().size(), 3);
        Assert.assertTrue(httpRequest.getBody().contains("price"));
    }

    @Test
    public void 여러번에_걸쳐서_Stream이_도착() throws IOException {
        // given
        HttpRequestStreamHolder streamHolder = new HttpRequestStreamHolder();
        String rawRequest1 = "POST /food ";
        String rawRequest2 = "HTTP/1.1\r\n" +
                            "Host: localhost:8080\r\n" +
                            "Content";
        String rawRequest3 = "-Type: application/json\r\n" +
                                "Content-Length: 43\r\n" +
                                "\r\n" +
                                "{ \"foodName\": ";
        String rawRequest4 = "\"kimchi\", \"price\": 15000 }";
        String[] rawRequests = {rawRequest1, rawRequest2, rawRequest3, rawRequest4};

        // when
        int idx = 0;
        while(!streamHolder.isDone()) {
            String rawRequest = rawRequests[idx];
            InputStream inputStream = new ByteArrayInputStream(rawRequest.getBytes(StandardCharsets.UTF_8));
            ByteBuffer wrap = ByteBuffer.wrap(inputStream.readAllBytes());
            streamHolder.write(wrap);
            idx += 1;
        }
        HttpRequest httpRequest = streamHolder.build();

        // then
        Assert.assertEquals(httpRequest.getUri(), "/food");
        Assert.assertEquals(httpRequest.getHeaders().size(), 3);
        Assert.assertTrue(httpRequest.getBody().contains("price"));
    }

    @Test
    public void ContentLength가_헤더에_없는_경우() throws IOException {
        // given
        HttpRequestStreamHolder streamHolder = new HttpRequestStreamHolder();
        String rawRequest =
                "GET /index.html HTTP/1.1\r\n" +
                        "Host: localhost:8080\r\n" +
                        "\r\n";
        InputStream inputStream = new ByteArrayInputStream(rawRequest.getBytes(StandardCharsets.UTF_8));
        ByteBuffer wrap = ByteBuffer.wrap(inputStream.readAllBytes());
        // when
        streamHolder.write(wrap);
        HttpRequest httpRequest = streamHolder.build();
        // then
        Assert.assertEquals(httpRequest.getUri(), "/index.html");
        Assert.assertEquals(httpRequest.getHeaders().size(), 1);
    }

    @Test
    public void ContentLength가_헤더에_없이_나눠서_도착() throws IOException {
        // given
        HttpRequestStreamHolder streamHolder = new HttpRequestStreamHolder();
        String rawRequest1 =
                "GET /in";
        String rawRequest2 = "dex.html HTTP/1.1\r\n";
        String rawRequest3 = "";
        String rawRequest4 = "Host: localhost:8080\r\n" +
                "\r\n";
        String[] rawRequests = {rawRequest1, rawRequest2, rawRequest3, rawRequest4};

        // when
        int idx = 0;
        while(!streamHolder.isDone()) {
            String rawRequest = rawRequests[idx];
            InputStream inputStream = new ByteArrayInputStream(rawRequest.getBytes(StandardCharsets.UTF_8));
            ByteBuffer wrap = ByteBuffer.wrap(inputStream.readAllBytes());
            streamHolder.write(wrap);
            idx++;
        }

        HttpRequest httpRequest = streamHolder.build();
        // then
        Assert.assertEquals(httpRequest.getUri(), "/index.html");
        Assert.assertEquals(httpRequest.getHeaders().size(), 1);
    }
}
