package org.example.server.processor;

import org.example.http.HttpRequest;
import org.example.http.HttpResponse;
import org.example.server.config.ServerConfig;
import org.example.server.config.ThreadConfig;
import org.example.server.config.ThreadConfigContext;
import org.example.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;


public class HttpRequestProcessor extends RequestProcessor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(HttpRequestProcessor.class);
    protected Socket connection;
    public HttpRequestProcessor(Socket connection) throws IOException {
        this.connection = connection;
    }
    @Override
    public void run() {
        HttpRequest httpRequest = null;
        HttpResponse httpResponse = null;
        try {
            httpRequest = this.getHttpRequest(this.connection);
            httpResponse = this.getHttpResponse(this.connection);
            ThreadConfigContext.set(new ThreadConfig(httpRequest.getHeaders().get("Host"), ServerConfig.getInstance()));

            String fileName = ThreadConfigContext.get().getIndexFileName();
            File rootDirectory = new File(ThreadConfigContext.get().getDocumentRoot());
            if(rootDirectory.isFile()) {
                throw new IllegalArgumentException("rootDirectory must be a directory, not a file");
            }
            rootDirectory = rootDirectory.getCanonicalFile();
            File theFile = FileUtils.find(rootDirectory, fileName);
            if (theFile.canRead()) {
                logger.info("{} {}", "GET", fileName);
                byte[] theData = Files.readAllBytes(theFile.toPath());
                httpResponse.setBody(theData);
            }

        } catch (IOException ex) {
            logger.warn("Socket Error" + connection.getRemoteSocketAddress(), ex);
        } catch (Exception ex) {
            logger.error("Unknown Error", ex);
        } finally {
            try {
                this.finish(this.connection, httpRequest, httpResponse);
            } catch (IOException ex) {
                logger.warn("Finishing Http Request Fails", ex);
            }
        }
    }
}
