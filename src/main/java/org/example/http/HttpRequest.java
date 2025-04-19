package org.example.http;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private String method;
    private String uri;
    private Map<String, String> requestParams;
    private String version;
    private Map<String, String> headers;
    private String body;

    public HttpRequest(String method, String uri, Map<String, String> requestParams, String version, Map<String, String> headers, String body) {
        this.method = method;
        this.uri = uri;
        this.requestParams = requestParams;
        this.version = version;
        this.headers = headers;
        this.body = body;
    }

    public HttpRequest(String method, String uri, Map<String, String> requestParams, String version, Map<String, String> headers) {
        this.method = method;
        this.uri = uri;
        this.requestParams = requestParams;
        this.version = version;
        this.headers = headers;
    }

    public String getMethod() {
        return method;
    }
    public String getUri() {
        return uri;
    }

    public String getParameter(String key) {
        if(this.requestParams.containsKey(key)) {
            return this.requestParams.get(key);
        }
        return "";
    }

    public String getVersion() {
        return version;
    }
    public Map<String, String> getHeaders() {
        return headers;
    }
    public String getBody() {
        return body;
    }

    public void plugBody(InputStream inputStream, int contentLength) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        char[] cArr = new char[contentLength];
        reader.read(cArr,0, contentLength);
        this.body = new String(cArr).trim();
    }

    public static HttpRequest buildHeader(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String requestLine = reader.readLine();
        if(requestLine == null) {
            throw new IllegalArgumentException("Empty InputStream");
        }

        String[] linePart = requestLine.split(" ");
        String method = linePart[0].trim();
        String uri = linePart[1].trim();

        String[] versionToken = linePart[2].trim().split("/");
        if(versionToken.length !=  2) {
            throw new IllegalArgumentException("Invalid HTTP Version Format");
        }
        String version = versionToken[1];


        Map<String, String> requestParams = new HashMap<>();
        int idx = 0;
        while(idx < uri.length()) {
            if(uri.charAt(idx) == '?') break;
            idx++;
        }

        if(idx + 1  < uri.length()) {
            String requestParamString  = uri.substring(idx + 1).trim();
            uri = uri.substring(0, idx).trim();
            String[] params = requestParamString.split("&");
            for(String param : params) {
                String[] s = param.trim().split("=");
                if(s.length != 2) {
                    throw new IllegalArgumentException("Invalid Request Param Format");
                }
                String key = s[0].trim();
                String val = s[1].trim();
                requestParams.put(key, val);
            }
        }

        String headerLine;
        Map<String, String> headers = new HashMap<>();
        while(!(headerLine = reader.readLine()).isEmpty()) {
            String[] header = headerLine.split(":");
            headers.put(header[0].trim(), header[1].trim());
        }
        return new HttpRequest(method, uri, requestParams, version, headers);
    }

    public static HttpRequest parse(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String requestLine = reader.readLine();
        if(requestLine == null) {
            throw new IllegalArgumentException("Empty InputStream");
        }

        String[] linePart = requestLine.split(" ");
        String method = linePart[0].trim();
        String uri = linePart[1].trim();

        String[] versionToken = linePart[2].trim().split("/");
        if(versionToken.length !=  2) {
            throw new IllegalArgumentException("Invalid HTTP Version Format");
        }
        String version = versionToken[1];


        Map<String, String> requestParams = new HashMap<>();
        int idx = 0;
        while(idx < uri.length()) {
            if(uri.charAt(idx) == '?') break;
            idx++;
        }

        if(idx + 1  < uri.length()) {
            String requestParamString  = uri.substring(idx + 1).trim();
            uri = uri.substring(0, idx).trim();
            String[] params = requestParamString.split("&");
            for(String param : params) {
                String[] s = param.trim().split("=");
                if(s.length != 2) {
                    throw new IllegalArgumentException("Invalid Request Param Format");
                }
                String key = s[0].trim();
                String val = s[1].trim();
                requestParams.put(key, val);
            }
        }

        String headerLine;
        Map<String, String> headers = new HashMap<>();
        while(!(headerLine = reader.readLine()).isEmpty()) {
            String[] header = headerLine.split(":");
            headers.put(header[0].trim(), header[1].trim());
        }

        String body = "";
        if(headers.containsKey("Content-Length")) {
            int len = Integer.parseInt(headers.get("Content-Length"));
            char[] cArr = new char[len];
            reader.read(cArr,0, len);
            body = new String(cArr).trim();
        }

        return new HttpRequest(method, uri, requestParams, version, headers, body);
    }
}
