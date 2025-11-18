package com.http.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    private byte[] body; //POST 请求体

    //构造
    public HttpRequest() {
        headers = new HashMap<>();
    }
    public HttpRequest(String method, String uri, String version, Map<String, String> headers, byte[] body) {
        this.method = method;
        this.uri = uri;
        this.version = version;
        this.headers = (headers != null) ? headers : new HashMap<>();//防止传入null
        this.body = body;
    }
    public HttpRequest(String method, String uri){
        this(method, uri, "HTTP/1.1", null, null);
    }

    public HttpRequest(InputStream inputStream) throws IOException {
        this.headers = new HashMap<>();

        // 完全避免BufferedReader，手动解析HTTP头部
        java.io.ByteArrayOutputStream headerBuffer = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1];
        boolean foundHeaderEnd = false;

        // 读取头部数据直到遇到\r\n\r\n
        while (!foundHeaderEnd) {
            int bytesRead = inputStream.read(buffer);
            if (bytesRead == -1) break;

            byte b = buffer[0];
            headerBuffer.write(b);

            // 检查是否到达头部结尾
            byte[] headerBytes = headerBuffer.toByteArray();
            int len = headerBytes.length;
            if (len >= 4) {
                if (headerBytes[len-4] == '\r' && headerBytes[len-3] == '\n' &&
                    headerBytes[len-2] == '\r' && headerBytes[len-1] == '\n') {
                    foundHeaderEnd = true;
                }
            } else if (len >= 2) {
                if (headerBytes[len-2] == '\n' && headerBytes[len-1] == '\n') {
                    foundHeaderEnd = true;
                }
            }
        }

        // 解析头部
        String headerString = headerBuffer.toString("UTF-8");
        String[] lines = headerString.split("\r?\n");

        if (lines.length == 0) {
            throw new IOException("无效的HTTP请求：请求行为空");
        }

        try {
            // 解析请求行
            String requestLine = lines[0].trim();
            String[] parts = requestLine.split(" ");
            if (parts.length >= 3) {
                this.method = parts[0];
                this.uri = parts[1];
                this.version = parts[2];
            } else {
                throw new IOException("无效的HTTP请求行格式: " + requestLine);
            }

            // 解析请求头
            int contentLength = 0;
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (!line.isEmpty()) {
                    String[] headerParts = line.split(": ", 2);
                    if (headerParts.length == 2) {
                        this.headers.put(headerParts[0], headerParts[1]);
                        if ("Content-Length".equalsIgnoreCase(headerParts[0])) {
                            contentLength = Integer.parseInt(headerParts[1]);
                        }
                    }
                }
            }


            // 读取请求体 - 使用更大的缓冲区
            if (contentLength > 0) {
                this.body = new byte[contentLength];
                int totalRead = 0;
                byte[] readBuffer = new byte[8192]; // 8KB 缓冲区

                long startTime = System.currentTimeMillis();
                int maxWaitTime = 10000; // 10秒总超时

                while (totalRead < contentLength) {
                    // 检查总超时
                    if (System.currentTimeMillis() - startTime > maxWaitTime) {
                        break;
                    }

                    int toRead = Math.min(readBuffer.length, contentLength - totalRead);

                    int bytesRead;
                    try {
                        bytesRead = inputStream.read(readBuffer, 0, toRead);
                    } catch (java.io.InterruptedIOException e) {
                        break;
                    }

                    if (bytesRead == -1) {
                        break;
                    }

                    if (bytesRead > 0) {
                        // 将读取的数据复制到body数组中
                        System.arraycopy(readBuffer, 0, this.body, totalRead, bytesRead);
                        totalRead += bytesRead;
                    } else {

                        // 如果剩余数据很少，可能是网络传输问题，尝试几次后放弃
                        int remaining = contentLength - totalRead;
                        if (remaining < 100) {
                            long smallWaitStart = System.currentTimeMillis();

                            while (remaining > 0 && (System.currentTimeMillis() - smallWaitStart) < 2000) {
                                try {
                                    int finalRead = inputStream.read(readBuffer, 0, Math.min(remaining, readBuffer.length));
                                    if (finalRead > 0) {
                                        System.arraycopy(readBuffer, 0, this.body, totalRead, finalRead);
                                        totalRead += finalRead;
                                        remaining = contentLength - totalRead;
                                    } else {
                                        Thread.sleep(50);
                                    }
                                } catch (Exception e) {
                                    break;
                                }
                            }

                            if (remaining > 0) {
                            }
                            break; // 无论如何都结束
                        } else {
                            // 给一点时间让数据到达
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }

                if (totalRead < contentLength) {
                    byte[] actualBody = new byte[totalRead];
                    System.arraycopy(this.body, 0, actualBody, 0, totalRead);
                    this.body = actualBody;
                }

            }

        } catch (java.net.SocketException e) {
            throw new IOException("客户端连接已断开: " + e.getMessage(), e);
        }
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
            request.setBody(bodyBuilder.toString().getBytes());
        }

        return request;
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public Map<String, String> getBodyParams() {
    Map<String, String> params = new HashMap<>();
    if (body == null || body.length == 0) return params;

    String bodyStr = new String(body, StandardCharsets.UTF_8);
    String[] pairs = bodyStr.split("&");
    for (String pair : pairs) {
        String[] kv = pair.split("=", 2);
        if (kv.length == 2) {
            // URL decode 可以加上 java.net.URLDecoder.decode(kv[0], "UTF-8")
            params.put(kv[0], kv[1]);
        }
    }
    return params;
}

    public String getParam(String key) {
        return getBodyParams().get(key);
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

    public byte[] getBody() { return body; }
    public void setBody(byte[] body) { this.body = body; }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        // 请求行
        builder.append(method).append(" ").append(uri).append(" ").append(version).append("\r\n");

        // 请求头
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }

        // 空行
        builder.append("\r\n");

        // 请求体
        if (body != null && body.length > 0) {
            builder.append(new String(body));
        }

        return builder.toString();
    }
}



