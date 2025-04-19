package org.example.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class HttpRequestStreamHolder {
    private ByteArrayOutputStream stream;
    private int delimiterStartIdx;
    private boolean isDone;
    private int ptr;
    private HttpRequest httpRequest;

    public HttpRequestStreamHolder() {
        this.stream = new ByteArrayOutputStream();
        this.delimiterStartIdx = -1;
        this.isDone = false;
        this.ptr = 0;
        this.httpRequest = null;
    }

    public void write(ByteBuffer byteBuffer) throws IOException {
        if(this.isDone()) return;

        this.stream.write(byteBuffer.array());
        byte[] byteStream = this.stream.toByteArray();

        if(this.httpRequest != null) {
            byte[] bodyBytes = Arrays.copyOfRange(byteStream, this.delimiterStartIdx, byteStream.length - 1);
            if(bodyBytes.length == Integer.parseInt(this.httpRequest.getHeaders().get("Content-Length"))) {
                this.httpRequest.plugBody(new ByteArrayInputStream(bodyBytes), bodyBytes.length);
                this.isDone = true;
            }
            return;
        }

        this.delimiterStartIdx = findDelimiter();
        if(this.delimiterStartIdx == -1) {
            return;
        }
        byte[] headerBytes = Arrays.copyOfRange(byteStream, 0, this.delimiterStartIdx+3);
        this.httpRequest = HttpRequest.buildHeader(new ByteArrayInputStream(headerBytes));

        if(this.httpRequest.getHeaders().containsKey("Content-Length")) {
            byte[] bodyBytes = Arrays.copyOfRange(byteStream, this.delimiterStartIdx, byteStream.length-1);
            if(bodyBytes.length == Integer.parseInt(this.httpRequest.getHeaders().get("Content-Length"))) {
                this.httpRequest.plugBody(new ByteArrayInputStream(bodyBytes), bodyBytes.length);
                this.isDone = true;
            }
        } else {
            this.isDone = true;
        }
    }

    public int findDelimiter() {
        byte[] streamByteArray = this.stream.toByteArray();
        while(this.ptr + 3 < streamByteArray.length) {
            byte b0 = streamByteArray[this.ptr];
            byte b1 = streamByteArray[this.ptr+1];
            byte b2 = streamByteArray[this.ptr+2];
            byte b3 = streamByteArray[this.ptr+3];
            if(b0 == 13 && b1 == 10 && b2 == 13 && b3 == 10) {
                this.delimiterStartIdx = this.ptr;
                return this.delimiterStartIdx;
            }
            this.ptr += 1;
        }
        return -1;
    }

    public boolean isDone() {
        return this.isDone;
    }

    public HttpRequest build() {
        if(!this.isDone()) {
            throw new IllegalStateException("Http Request Not Built Completely");
        }
        return this.httpRequest;
    }
}
