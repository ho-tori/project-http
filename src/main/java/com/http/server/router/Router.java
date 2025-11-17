package com.http.server.router;

import com.http.common.HttpRequest;
import com.http.common.HttpResponse;
import com.http.server.handler.LoginHandler;
import com.http.server.handler.RegisterHandler;
import com.http.server.handler.StaticFileHandler;
import com.http.server.handler.FileUploadHandler;

public class Router {
    private final LoginHandler loginHandler;
    private final RegisterHandler registerHandler;
    private final StaticFileHandler staticFileHandler;
    private final FileUploadHandler fileUploadHandler;

    public Router() {
        this.loginHandler = new LoginHandler();
        this.registerHandler = new RegisterHandler();
        this.staticFileHandler = new StaticFileHandler();
        this.fileUploadHandler = new FileUploadHandler();
    }

    /**
     * 根据请求路径和方法，路由到对应的处理器
     */
    public HttpResponse route(HttpRequest request) {
        String uri = request.getUri();
        String method = request.getMethod();

        // 重定向示例 - /old-page 重定向到 /new-page.html
        if ("/old-page".equals(uri) || "/redirect-test".equals(uri)) {
            return createRedirectResponse("/new-page.html", 301);
        }

        // API路由 - 支持 /login 和 /api/login 两种格式
        if (("/login".equals(uri) || "/api/login".equals(uri)) && "POST".equalsIgnoreCase(method)) {
            return loginHandler.handle(request);
        } else if (("/register".equals(uri) || "/api/register".equals(uri)) && "POST".equalsIgnoreCase(method)) {
            return registerHandler.handle(request);
        } else if (("/upload".equals(uri) || "/api/upload".equals(uri)) && "POST".equalsIgnoreCase(method)) {
            return fileUploadHandler.handle(request);
        } 
        // 默认：静态文件处理
        else {
            return staticFileHandler.handle(request);
        }
    }

    /**
     * 创建重定向响应
     */
    private HttpResponse createRedirectResponse(String location, int statusCode) {
        HttpResponse response = new HttpResponse();
        response.setVersion("HTTP/1.1");
        response.setStatusCode(statusCode);
        response.setReasonPhrase(statusCode == 301 ? "Moved Permanently" : "Found");
        response.addHeader("Location", location);
        response.addHeader("Connection", "close");
        
        String body = "<html><body><h1>Page Moved</h1><p>This page has moved to <a href=\"" + location + "\">" + location + "</a></p></body></html>";
        response.setBody(body);
        response.addHeader("Content-Type", "text/html");
        response.addHeader("Content-Length", String.valueOf(body.length()));
        
        return response;
    }
}
