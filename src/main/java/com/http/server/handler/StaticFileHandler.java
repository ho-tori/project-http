package com.http.server.handler;

import com.http.common.HttpRequest;
import com.http.common.HttpResponse;
import com.http.common.HttpStatus;
import com.http.common.MimeType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StaticFileHandler {
    // 静态文件根目录（相对于项目根目录）
    private final Path webRoot;

    public StaticFileHandler() {
        // 默认指向 src/main/java/web 目录
        this(Paths.get("src", "main", "java", "web"));
    }

    public StaticFileHandler(Path webRoot) {
        this.webRoot = webRoot.toAbsolutePath().normalize();
    }

    /**
     * 处理静态资源请求，仅支持 GET/HEAD
     */
    public HttpResponse handle(HttpRequest request) {
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            return buildMethodNotAllowed();
        }

        String uri = request.getUri();
        if (uri == null || uri.isEmpty()) {
            uri = "/";
        }

        // 目录默认返回 index.html
        if ("/".equals(uri)) {
            uri = "/index.html";
        }

        // 安全处理：去除前导斜杠并阻止路径穿越
        String safePath = uri.replaceFirst("^/", "");
        if (safePath.contains("..")) {
            return buildNotFound();
        }

        Path target = webRoot.resolve(safePath).normalize();
        // 确保请求路径仍在 webRoot 下
        if (!target.startsWith(webRoot)) {
            return buildNotFound();
        }

        File file = target.toFile();
        if (!file.exists() || !file.isFile()) {
            return buildNotFound();
        }

        try {
            byte[] content = Files.readAllBytes(target);
            String contentType = MimeType.getMimeType(file.getName());

            HttpResponse resp = new HttpResponse();
            resp.setVersion("HTTP/1.1");
            resp.setStatusCode(HttpStatus.OK);
            resp.setReasonPhrase(HttpStatus.getReasonPhrase(HttpStatus.OK));
            resp.addHeader("Content-Type", contentType);
            resp.addHeader("Content-Length", String.valueOf(content.length));
            resp.addHeader("Connection", "close");

            if (!"HEAD".equalsIgnoreCase(method)) {
                resp.setBody(content);
            }
            return resp;
        } catch (IOException e) {
            return buildInternalError();
        }
    }

    private HttpResponse buildNotFound() {
        Path notFoundPage = webRoot.resolve("404.html");
        HttpResponse resp = new HttpResponse();
        resp.setVersion("HTTP/1.1");
        resp.setStatusCode(HttpStatus.NOT_FOUND);
        resp.setReasonPhrase(HttpStatus.getReasonPhrase(HttpStatus.NOT_FOUND));
        resp.addHeader("Connection", "close");

        if (Files.exists(notFoundPage)) {
            try {
                byte[] body = Files.readAllBytes(notFoundPage);
                resp.addHeader("Content-Type", "text/html");
                resp.addHeader("Content-Length", String.valueOf(body.length));
                resp.setBody(body);
                return resp;
            } catch (IOException ignored) {
                // fall through to plain text body
            }
        }

        byte[] body = "404 Not Found".getBytes();
        resp.addHeader("Content-Type", "text/plain");
        resp.addHeader("Content-Length", String.valueOf(body.length));
        resp.setBody(body);
        return resp;
    }

    private HttpResponse buildMethodNotAllowed() {
        byte[] body = "405 Method Not Allowed".getBytes();
        HttpResponse resp = new HttpResponse();
        resp.setVersion("HTTP/1.1");
        resp.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
        resp.setReasonPhrase(HttpStatus.getReasonPhrase(HttpStatus.METHOD_NOT_ALLOWED));
        resp.addHeader("Content-Type", "text/plain");
        resp.addHeader("Content-Length", String.valueOf(body.length));
        resp.addHeader("Connection", "close");
        resp.setBody(body);
        return resp;
    }

    private HttpResponse buildInternalError() {
        byte[] body = "500 Internal Server Error".getBytes();
        HttpResponse resp = new HttpResponse();
        resp.setVersion("HTTP/1.1");
        resp.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        resp.setReasonPhrase(HttpStatus.getReasonPhrase(HttpStatus.INTERNAL_SERVER_ERROR));
        resp.addHeader("Content-Type", "text/plain");
        resp.addHeader("Content-Length", String.valueOf(body.length));
        resp.addHeader("Connection", "close");
        resp.setBody(body);
        return resp;
    }
}