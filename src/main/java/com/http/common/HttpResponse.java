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
    private byte[] body; //响应体

    //构造
    public HttpResponse() {
        headers = new HashMap<>();
    }
    public HttpResponse(String version, int statusCode, String reasonPhrase, Map<String, String> headers, byte[] body) {
        this.version = version;
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
        this.headers = (headers != null) ? headers : new HashMap<>();//防止传入null
        this.body = body;
    }


    public static HttpResponse parse(String responseString) {
        HttpResponse response = new HttpResponse();
        String[] lines = responseString.split("\r\n");

        if (lines.length == 0) {
            return null;
        }

        // 解析状态行
        String[] requestLine = lines[0].split(" ");
        if (requestLine.length >= 3) {
            response.setVersion(requestLine[0]);
            response.setStatusCode(Integer.parseInt(requestLine[1]));
            response.setReasonPhrase(requestLine[2]);
        }

        // 解析响应头
        int bodyStartIndex = -1;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) {
                bodyStartIndex = i + 1;
                break;
            }//如果有空行，则请求头结束

            String[] headerParts = lines[i].split(": ", 2);
            if (headerParts.length == 2) {
                response.addHeader(headerParts[0], headerParts[1]);
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
            response.setBody(bodyBuilder.toString().getBytes());
        }

        return response;
    }
    public void addHeader(String name, String value) {
        headers.put(name, value);
    }
    public String getHeader(String name) {
        return headers.get(name);
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

    public byte[] getBody() { return body; }
    public void setBody(byte[] body) { this.body = body; }
    public void setBody(String body) {
        this.body = body.getBytes();
    }

    public byte[] toBytes() {
        StringBuilder builder = new StringBuilder();
        // 状态行
        builder.append(version).append(" ").append(statusCode).append(" ").append(reasonPhrase).append("\r\n");
        // 响应头
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        builder.append("\r\n"); // 空行

        // 响应体
        byte[] headerBytes = builder.toString().getBytes();
        if (body != null && body.length > 0) {
            byte[] responseBytes = new byte[headerBytes.length + body.length];
            System.arraycopy(headerBytes, 0, responseBytes, 0, headerBytes.length);
            System.arraycopy(body, 0, responseBytes, headerBytes.length, body.length);
            return responseBytes;
        } else {
            return headerBytes;
        }
    }
}
