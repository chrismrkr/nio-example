package org.example.server.config;

import java.util.List;

public class ThreadConfig {
    private int port;
    private int threadPoolSize;
    private String host;
    private String documentRoot;
    private String indexFileName;
    private boolean isKeepAlive = false;
    private List<String> allowExt;
    private List<String> blockExt;
    private String error_403;
    private String error_404;
    private String error_500;

    public int getPort() {
        return port;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public String getHost() {
        return host;
    }

    public String getDocumentRoot() {
        return documentRoot;
    }

    public String getIndexFileName() {
        return indexFileName;
    }
    public boolean isKeepAlive() {
        return isKeepAlive;
    }
    public List<String> getAllowExt() {
        return allowExt;
    }

    public List<String> getBlockExt() {
        return blockExt;
    }

    public String getError_403() {
        return error_403;
    }

    public String getError_404() {
        return error_404;
    }

    public String getError_500() {
        return error_500;
    }

    public void setKeepAlive() {
        isKeepAlive = true;
    }
    public ThreadConfig(String hostName, ServerConfig serverConfig) {
        this.port = serverConfig.getPort();
        this.threadPoolSize = serverConfig.getThreadPoolSize();
        this.allowExt = serverConfig.getAllowExt();
        this.blockExt = serverConfig.getBlockExt();

        if(hostName.equals(serverConfig.getDefaultHost())) {
            this.host = serverConfig.getDefaultHost();
            this.documentRoot = serverConfig.getDocumentRoot();
            this.indexFileName = serverConfig.getIndexFileName();
            this.error_403 = serverConfig.getError_403();
            this.error_404 = serverConfig.getError_404();
            this.error_500 = serverConfig.getError_500();
        } else {
            for(ServerConfig.VirtualHostConfig virtualHostConfig : serverConfig.getVirtualHosts()) {
                if(virtualHostConfig.getVirtualHost().equals(hostName)) {
                    this.host = virtualHostConfig.getVirtualHost();
                    this.documentRoot = virtualHostConfig.getDocumentRoot();
                    this.indexFileName = virtualHostConfig.getIndexFileName();
                    this.error_403 = virtualHostConfig.getError_403();
                    this.error_404 = virtualHostConfig.getError_404();
                    this.error_500 = virtualHostConfig.getError_500();
                }
            }
        }
    }
}
