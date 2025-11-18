package com.http.server.handler;

import com.http.common.HttpRequest;
import com.http.common.HttpResponse;
import com.http.common.HttpStatus;
import com.http.server.auth.UserManager;

public class LoginHandler {
    //处理 login 的 POST 请求
    //调用 UserManager 进行验证，返回登录结果的 HttpResponse
    //若验证成功返回 200，否则返回 401 或 403
    public HttpResponse handle(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        response.setVersion("HTTP/1.1");

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
            response.setReasonPhrase(HttpStatus.getReasonPhrase(HttpStatus.METHOD_NOT_ALLOWED));
            response.setBody("405 Method Not Allowed");
            return response;
        }

        // 解析JSON请求体
        String body = request.getBody() == null ? "" : new String(request.getBody(), java.nio.charset.StandardCharsets.UTF_8);
        String username = extractJsonField(body, "username");
        String password = extractJsonField(body, "password");

        if (username == null || password == null) {
            response.setStatusCode(400); // BAD_REQUEST
            response.setReasonPhrase("Bad Request");
            response.setBody("Missing username or password in JSON body.");
            return response;
        }

        if (UserManager.getInstance("src/main/java/com/http/server/auth/users.json").login(username, password)) {
            response.setStatusCode(HttpStatus.OK);
            response.setReasonPhrase(HttpStatus.getReasonPhrase(HttpStatus.OK));
            response.setBody("Login successful!");
        } else {
            response.setStatusCode(HttpStatus.NOT_FOUND);
            response.setReasonPhrase(HttpStatus.getReasonPhrase(HttpStatus.NOT_FOUND));
            response.setBody("Invalid credentials.");
        }

        // 设置响应头
        response.addHeader("Content-Type", "text/plain; charset=UTF-8");
        response.addHeader("Content-Length", String.valueOf(response.getBody().length));
        response.addHeader("Connection", "close");

        return response;
    }

    /**
     * 简单的JSON字段提取方法
     */
    private String extractJsonField(String json, String key) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\"(.*?)\"");
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
}