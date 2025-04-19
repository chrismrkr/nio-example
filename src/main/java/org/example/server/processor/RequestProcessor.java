
package org.example.server.processor;

import org.example.http.HttpResponse;
import org.example.server.IdleConnectionManager;
import org.example.server.config.ServerConfig;
import org.example.server.config.ThreadConfig;
import org.example.server.config.ThreadConfigContext;
import org.example.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

public abstract class RequestProcessor {
    protected HttpRequest getHttpRequest(Socket socket) throws IOException {
        InputStream inputStream = socket.getInputStream();
        return HttpRequest.parse(inputStream);
    }

    protected HttpResponse getHttpResponse(Socket socket) throws IOException {
        return new HttpResponse(socket.getOutputStream());
    }

    protected void finish(Socket socket, HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
        boolean isKeepAlive = ThreadConfigContext.get().isKeepAlive();
        httpResponse.flush();
        ThreadConfigContext.clear();
        if(Float.parseFloat(httpRequest.getVersion()) < 1.1) {
            socket.close();
            return;
        }

        // TODO. Keep-Alive connection 처리
//        if(isKeepAlive) {
//            IdleConnectionManager.reserveConnection(this.connection);
//            return;
//        }
        socket.close();
    }
}