package com.http.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
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

    public static HttpResponse parse(InputStream inputStream) throws IOException {
        HttpResponse response = new HttpResponse();
        response.setHeaders(new HashMap<>());

        // ---1. 读取Header ---
        ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1];
        boolean foundHeaderEnd = false;

        while(!foundHeaderEnd){
            int bytesRead = inputStream.read(buffer);
            if (bytesRead == -1) break;

            byte b = buffer[0];
            headerBuffer.write(b);

            byte[] headerBytes = headerBuffer.toByteArray();
            int len = headerBytes.length;

            if (len >= 4) {
                if (headerBytes[len - 4] == '\r' && headerBytes[len - 3] == '\n'
                        && headerBytes[len - 2] == '\r' && headerBytes[len - 1] == '\n') {
                    foundHeaderEnd = true;
                }
            } else if (len >= 2) {
                if (headerBytes[len - 2] == '\n' && headerBytes[len - 1] == '\n') {
                    foundHeaderEnd = true;
                }
            }

        }

        // --- 2.解析Header ---
        String headerString = headerBuffer.toString("UTF-8");
        String[] lines = headerString.split("\r?\n");

        if (lines.length == 0) {
            throw new IOException("无效的HTTP响应：响应行为空");
        }

        try{
            String responseLine = lines[0].trim();
            String[] parts = responseLine.split(" ", 3);
            if (parts.length >= 3) {
                response.setVersion(parts[0]);
                response.setStatusCode(Integer.parseInt(parts[1]));
                response.setReasonPhrase(parts[2]);
            } else {
                throw new IOException("无效的HTTP响应行格式: " + responseLine);
            }

            int contentLength = 0;
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if(!line.isEmpty()){
                    String[] headerParts = line.split(": ", 2);
                    if(headerParts.length == 2){
                        response.addHeader(headerParts[0], headerParts[1]);
                        if("Content-Length".equalsIgnoreCase(headerParts[0])){
                            contentLength = Integer.parseInt(headerParts[1]);
                        }
                    }
                }
            }
            if(contentLength > 0) {
                byte[] bodyBytes = new byte[contentLength];
                int totalRead = 0;
                while (totalRead < contentLength) {
                    int read = inputStream.read(bodyBytes, totalRead, contentLength - totalRead);
                    if (read == -1) {
                        throw new IOException("未能读取完整的响应体：期望 " + contentLength + " 字节，但只读取了 " + totalRead + " 字节");
                    }
                    totalRead += read;
                }
                response.setBody(bodyBytes);
            }
        }catch (NumberFormatException e){
            throw new IOException("无效的状态码或Content-length: " + e.getMessage(), e);
        }catch (SocketException e){
            throw new IOException("连接已断开: " + e.getMessage(), e);
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
