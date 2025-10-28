package com.http.server.handler;

import com.http.common.HttpRequest;
import com.http.common.HttpResponse;
import com.http.common.HttpStatus;
import com.http.server.auth.UserManager;

import java.util.HashMap;
import java.util.Map;

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

        String username = request.getParam("username");
        String password = request.getParam("password");
        //todo:接口可能还要再处理一下

        if (username == null || password == null) {
            response.setStatusCode(HttpStatus.NOT_FOUND);
            response.setReasonPhrase(HttpStatus.getReasonPhrase(HttpStatus.NOT_FOUND));
            response.setBody("Missing username or password.");
            return response;
        }

        if (UserManager.getInstance().login(username, password)) {
            response.setStatusCode(HttpStatus.OK);
            response.setReasonPhrase(HttpStatus.getReasonPhrase(HttpStatus.OK));
            response.setBody("Login successful!");
        } else {
            response.setStatusCode(HttpStatus.NOT_FOUND);
            response.setReasonPhrase(HttpStatus.getReasonPhrase(HttpStatus.NOT_FOUND));
            response.setBody("Invalid credentials.");
        }

        // 设置响应头
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain; charset=UTF-8");
        headers.put("Content-Length", String.valueOf(response.getBody().length));
        response.setHeaders(headers);

        return response;
    }
}