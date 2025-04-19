package org.example.server.config;

import org.example.util.ConfigLoader;

import java.io.IOException;
import java.util.List;

public class ServerConfig {
    private int port;
    private int threadPoolSize;
    private String defaultHost;
    private String documentRoot;
    private String indexFileName;
    private List<String> allowExt;
    private List<String> blockExt;
    private String error_403;
    private String error_404;
    private String error_500;
    private List<VirtualHostConfig> virtualHosts;

    public ServerConfig(int port, int threadPoolSize, String defaultHost, String documentRoot, String indexFileName, List<String> allowExt, List<String> blockExt, String error_403, String error_404, String error_500, List<VirtualHostConfig> virtualHosts) {
        this.port = port;
        this.threadPoolSize = threadPoolSize;
        this.defaultHost = defaultHost;
        this.documentRoot = documentRoot;
        this.indexFileName = indexFileName;
        this.allowExt = allowExt;
        this.blockExt = blockExt;
        this.error_403 = error_403;
        this.error_404 = error_404;
        this.error_500 = error_500;
        this.virtualHosts = virtualHosts;
    }

    protected ServerConfig() {
    }

    public int getPort() {
        return port;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public String getDefaultHost() {
        return defaultHost;
    }

    public String getDocumentRoot() {
        return documentRoot;
    }

    public String getIndexFileName() {
        return indexFileName;
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

    public List<VirtualHostConfig> getVirtualHosts() {
        return virtualHosts;
    }

    public static class VirtualHostConfig {
        private String virtualHost;
        private String documentRoot;
        private String indexFileName;
        private String error_403;
        private String error_404;
        private String error_500;
        public VirtualHostConfig(String virtualHost, String documentRoot, String indexFileName, String error_403, String error_404, String error_500) {
            this.virtualHost = virtualHost;
            this.documentRoot = documentRoot;
            this.indexFileName = indexFileName;
            this.error_403 = error_403;
            this.error_404 = error_404;
            this.error_500 = error_500;
        }

        public VirtualHostConfig() {
        }

        public String getVirtualHost() {
            return virtualHost;
        }

        public String getDocumentRoot() {
            return documentRoot;
        }

        public String getIndexFileName() {
            return indexFileName;
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
    }

    private static class Holder {
        private static ServerConfig serverConfig;
    }

    public static void createInstance(String filePath) throws IOException {
        Holder.serverConfig = ConfigLoader.load(filePath, ServerConfig.class);
    }
    public static ServerConfig getInstance() {
        return Holder.serverConfig;
    }

}
