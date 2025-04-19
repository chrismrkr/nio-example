package httpRequest;

import org.example.http.HttpResponse;
import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HttpResponseTest {
    @Test
    public void Response_생성_성공() throws IOException {
        // given
        String message = "Hello, World";
        OutputStream outputStream = new ByteArrayOutputStream();

        // when then
        HttpResponse httpResponse = new HttpResponse(outputStream);
        httpResponse.setBody(message);
        httpResponse.flush();
    }
}
