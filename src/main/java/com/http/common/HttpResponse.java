package com.http.common;

import java.nio.charset.StandardCharsets;
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

    // 添加 / 获取 header
    public void addHeader(String name, String value) {
        headers.put(name, value);
    }
    public String getHeader(String name) {
        return headers.get(name);
    }

    // -------------------------------
    // ✅ 可选：解析响应字符串（用于调试）
    // -------------------------------
    public static HttpResponse parse(String responseString) {
        HttpResponse response = new HttpResponse();
        String[] lines = responseString.split("\r\n");

        if (lines.length == 0) return null;

        // 解析状态行
        String[] requestLine = lines[0].split(" ", 3);
        if (requestLine.length >= 3) {
            response.setVersion(requestLine[0]);
            response.setStatusCode(Integer.parseInt(requestLine[1]));
            response.setReasonPhrase(requestLine[2]);
        }

        // 解析头部
        int bodyStartIndex = -1;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) {
                bodyStartIndex = i + 1;
                break;
            }
            String[] headerParts = lines[i].split(": ", 2);
            if (headerParts.length == 2) {
                response.addHeader(headerParts[0], headerParts[1]);
            }
        }

        // 解析响应体（仅限文本）
        if (bodyStartIndex != -1 && bodyStartIndex < lines.length) {
            StringBuilder bodyBuilder = new StringBuilder();
            for (int i = bodyStartIndex; i < lines.length; i++) {
                bodyBuilder.append(lines[i]);
                if (i < lines.length - 1) bodyBuilder.append("\r\n");
            }
            response.setBody(bodyBuilder.toString().getBytes());
        }

        return response;
    }


    // -------------------------------
    // ✅ Body 相关
    // -------------------------------

    // 设置文本响应体（自动转字节）
    public void setBody(String bodyText) {
        if (bodyText != null) {
            this.body = bodyText.getBytes(StandardCharsets.UTF_8);
            headers.put("Content-Length", String.valueOf(this.body.length));
        }
    }

    // 设置二进制响应体 非文本
    public void setBody(byte[] bodyBytes) {
        if (bodyBytes != null) {
            this.body = bodyBytes;
            headers.put("Content-Length", String.valueOf(this.body.length));
        }
    }

    public byte[] getBody() {
        return body;
    }

    // -------------------------------
    // ✅ 转为可发送的字节报文
    // -------------------------------
    public byte[] toBytes() {
        // 拼接响应头
        StringBuilder sb = new StringBuilder();
        sb.append(version)
                .append(" ")
                .append(statusCode)
                .append(" ")
                .append(reasonPhrase)
                .append("\r\n");

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append("\r\n");
        }

        sb.append("\r\n"); // 空行分隔头部与主体

        byte[] headerBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] bodyBytes = (body != null) ? body : new byte[0];

        // 合并 header 和 body
        byte[] full = new byte[headerBytes.length + bodyBytes.length];
        System.arraycopy(headerBytes, 0, full, 0, headerBytes.length);
        System.arraycopy(bodyBytes, 0, full, headerBytes.length, bodyBytes.length);

        return full;
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

}
