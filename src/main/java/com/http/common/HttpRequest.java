package com.http.common;

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



