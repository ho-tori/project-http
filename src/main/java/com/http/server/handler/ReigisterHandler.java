package com.http.server.handler;

import com.http.common.HttpRequest;
import com.http.common.HttpResponse;
import com.http.common.HttpStatus;
import com.http.server.auth.UserManager;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReigisterHandler {
    private final UserManager userManager = UserManager.getInstance();

    /**
     * 处理用户注册，仅支持 POST，Body 为简易 JSON：{"username":"...","password":"..."}
     */
    public HttpResponse handle(HttpRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return buildMethodNotAllowed();
        }

        String body = request.getBody() == null ? "" : new String(request.getBody(), StandardCharsets.UTF_8);
        String username = extractJsonField(body, "username");
        String password = extractJsonField(body, "password");

        if (username == null || password == null) {
            return buildBadRequest("缺少 username 或 password 字段");
        }

        boolean ok = userManager.register(username, password);
        if (ok) {
            String respBody = "{\"status\":\"ok\",\"message\":\"registered\"}";
            return buildJsonResponse(HttpStatus.OK, HttpStatus.getReasonPhrase(HttpStatus.OK), respBody);
        } else {
            String respBody = "{\"status\":\"error\",\"message\":\"用户名已存在或密码不合法\"}";
            // 使用 400 Bad Request（项目未定义常量，这里直接设置）
            return buildJsonResponse(400, "Bad Request", respBody);
        }
    }

    private String extractJsonField(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"(.*?)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private HttpResponse buildJsonResponse(int statusCode, String reason, String bodyStr) {
        byte[] body = bodyStr.getBytes(StandardCharsets.UTF_8);
        HttpResponse resp = new HttpResponse();
        resp.setVersion("HTTP/1.1");
        resp.setStatusCode(statusCode);
        resp.setReasonPhrase(reason);
        resp.addHeader("Content-Type", "application/json; charset=utf-8");
        resp.addHeader("Content-Length", String.valueOf(body.length));
        resp.addHeader("Connection", "close");
        resp.setBody(body);
        return resp;
    }

    private HttpResponse buildMethodNotAllowed() {
        String bodyStr = "{\"status\":\"error\",\"message\":\"Method Not Allowed\"}";
        return buildJsonResponse(HttpStatus.METHOD_NOT_ALLOWED,
                HttpStatus.getReasonPhrase(HttpStatus.METHOD_NOT_ALLOWED), bodyStr);
    }

    private HttpResponse buildBadRequest(String message) {
        String bodyStr = "{\"status\":\"error\",\"message\":\"" + message + "\"}";
        return buildJsonResponse(400, "Bad Request", bodyStr);
    }
}
