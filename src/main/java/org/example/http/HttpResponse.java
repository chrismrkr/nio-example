package org.example.http;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private String statusLine;
    private Map<String, String> headers = new HashMap<>();
    private byte[] body;
    private OutputStream outputStream;
    private Writer writer;

    public HttpResponse(OutputStream outputStream) {
        this.outputStream = new BufferedOutputStream(outputStream);
        this.writer = new OutputStreamWriter(outputStream);
    }

    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }
    public void setStatusLine(Integer statusCode, String phase) {
        this.statusLine = "HTTP/1.1 " + Integer.toString(statusCode) + " " + phase;
    }

    public void setBody(byte[] body) {
        this.body = body;
        this.addHeader("Content-Length", String.valueOf(body.length));
    }

    public void setBody(String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        this.setBody(bytes);
    }

    public byte[] getBody() {
        return body;
    }

    public void flush() throws IOException {
        String delimiter = "\r\n";

        if(this.statusLine == null || this.statusLine.equals("")) {
            this.setStatusLine(200, "OK");
        }
        this.writer.write(this.statusLine + delimiter);


        if(!this.headers.containsKey("Content-Type")) {
            this.addHeader("Content-Type", "text/html; charset=UTF-8");
        }
        for (Map.Entry<String, String> header : this.headers.entrySet()) {
            this.writer.write(header.getKey() + ": " + header.getValue() + delimiter);
        }

        this.writer.write(delimiter);
        this.writer.flush();

        if (body != null) {
            outputStream.write(body);
        }
        outputStream.flush();
    }
}
