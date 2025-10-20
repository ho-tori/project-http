package com.http.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    //封装HTTP请求报文
    /*eg:

    GET /index.html HTTP/1.1
    Host: localhost:8080
    User-Agent: Chrome
    Accept: text/html
    Connection: keep-alive
   （空行）
    请求体（如果是POST请求）
    */

    //属性
    private String method; //请求方法（GET、POST等）
    private String uri; //请求URI
    private String version; //HTTP版本

    private Map<String, String> headers; //请求头

    private String body; //POST 请求体

    //构造
    public HttpRequest() {
        headers = new HashMap<>();
    }
    public HttpRequest(String method, String uri, String version, Map<String, String> headers, String body) {
        this.method = method;
        this.uri = uri;
        this.version = version;
        this.headers = (headers != null) ? headers : new HashMap<>();//防止传入null
        this.body = body;
    }

    public static HttpRequest parse(String requestString) {
        HttpRequest request = new HttpRequest();
        String[] lines = requestString.split("\r\n");

        if (lines.length == 0) {
            return null;
        }

        // 解析请求行
        String[] requestLine = lines[0].split(" ");
        if (requestLine.length >= 3) {
            request.setMethod(requestLine[0]);
            request.setUri(requestLine[1]);
            request.setVersion(requestLine[2]);
        }

        // 解析请求头
        int bodyStartIndex = -1;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) {
                bodyStartIndex = i + 1;
                break;
            }//如果有空行，则请求头结束

            String[] headerParts = lines[i].split(": ", 2);
            if (headerParts.length == 2) {
                request.addHeader(headerParts[0], headerParts[1]);
            }
        }

        // 解析请求体
        if (bodyStartIndex != -1 && bodyStartIndex < lines.length) {
            StringBuilder bodyBuilder = new StringBuilder();
            for (int i = bodyStartIndex; i < lines.length; i++) {
                bodyBuilder.append(lines[i]);
                if (i < lines.length - 1) {
                    bodyBuilder.append("\r\n");
                }
            }
            request.setBody(bodyBuilder.toString());
        }

        return request;
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }



    //getter setter
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}



