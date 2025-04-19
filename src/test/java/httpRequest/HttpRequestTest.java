package httpRequest;

import org.example.http.HttpRequest;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class HttpRequestTest {
    @Test
    public void POST_요청_파싱() throws IOException {
        // given
        String rawRequest =
                "POST /food HTTP/1.1\r\n" +
                        "Host: localhost:8080\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: 43\r\n" +
                        "\r\n" +
                        "{ \"foodName\": \"kimchi\", \"price\": 15000 }";
        // when
        InputStream inputStream = new ByteArrayInputStream(rawRequest.getBytes(StandardCharsets.UTF_8));
        HttpRequest httpRequest = HttpRequest.parse(inputStream);

        // then
        Assert.assertEquals(httpRequest.getMethod(), "POST");
        Assert.assertEquals(httpRequest.getUri(), "/food");
        Assert.assertEquals(httpRequest.getVersion(), "1.1");
        Assert.assertEquals(httpRequest.getHeaders().size(), 3);
    }

    @Test
    public void GET_요청_파싱() throws IOException {
        // given
        String rawRequest =
                "GET /movie?type=science&name=interstellar HTTP/1.1\r\n" +
                        "Host: localhost:8080\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: 43\r\n" +
                        "\r\n";
        // when
        InputStream inputStream = new ByteArrayInputStream(rawRequest.getBytes(StandardCharsets.UTF_8));
        HttpRequest httpRequest = HttpRequest.parse(inputStream);

        // then
        Assert.assertEquals(httpRequest.getMethod(), "GET");

    }
}
