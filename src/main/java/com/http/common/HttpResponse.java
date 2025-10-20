package com.http.common;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    //封装HTTP响应报文
    /*eg:

    HTTP/1.1 200 OK
    Content-Type: text/html
    Content-Length: 27

    <html>Hello World</html>

    */

    //属性
    private String version; //HTTP版本
    private int statusCode; //状态码
    private String reasonPhrase; //状态描述
    private Map<String, String> headers; //响应头
    private String body; //响应体

    //构造
    public HttpResponse() {
        headers = new HashMap<>();
    }
    public HttpResponse(String version, int statusCode, String reasonPhrase, Map<String, String> headers, String body) {
        this.version = version;
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
        this.headers = (headers != null) ? headers : new HashMap<>();//防止传入null
        this.body = body;
    }

    //getter setter
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public String getReasonPhrase() { return reasonPhrase; }
    public void setReasonPhrase(String reasonPhrase) { this.reasonPhrase = reasonPhrase; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
