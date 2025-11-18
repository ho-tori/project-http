package com.http.server;

import java.io.*;
import java.net.*;
import com.http.common.HttpRequest;
import com.http.common.HttpResponse;
import com.http.server.router.Router;
import com.http.utils.ConsoleWriter;

public class ConnectionHandler implements Runnable{
    //每个客户端请求的“单独处理线程”
    //实现接口更灵活，可以继承别的类

    //①	从 socket 读取请求报文	使用 HttpRequest(InputStream) 解析
    //②	打印或理解请求信息	（调试或日志）
    //③	构造 HTTP 响应	使用 HttpResponse
    //④	发送响应回客户端	输出字节流并关闭 socket

    private Socket socket;
    private Router router;

    public ConnectionHandler(Socket socket) {
        this.socket = socket;
        this.router = new Router();
    }

    @Override
    public void run(){
        try {
            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();
            
            boolean keepAlive = true;
            
            // 支持长连接 - 在一个TCP连接上处理多个HTTP请求
            while (keepAlive) {
                try {
                    // 1️⃣ 解析请求
                    HttpRequest request = new HttpRequest(input);
                    String uri = request.getUri();
                    ConsoleWriter.logServer("收到请求: " + request.getMethod() + " " + uri);

                    // 2️⃣ 使用Router路由请求到对应的Handler
                    HttpResponse response = router.route(request);

                    // 3️⃣ 检查是否支持长连接
                    String connection = request.getHeaders().get("Connection");
                    if ("close".equalsIgnoreCase(connection) || 
                        "HTTP/1.0".equals(request.getVersion())) {
                        keepAlive = false;
                        response.addHeader("Connection", "close");
                    } else {
                        // HTTP/1.1 默认支持长连接
                        response.addHeader("Connection", "keep-alive");
                        response.addHeader("Keep-Alive", "timeout=30, max=100");
                    }

                    // 4️⃣ 发送响应
                    output.write(response.toBytes());
                    output.flush();
                    
                    ConsoleWriter.logServer("响应已发送: " + response.getStatusCode() + " " + response.getReasonPhrase());
                    
                    // 如果是短连接，退出循环
                    if (!keepAlive) {
                        break;
                    }
                    
                } catch (java.net.SocketTimeoutException e) {
                    ConsoleWriter.logServer("连接超时，关闭长连接");
                    break;
                } catch (java.io.EOFException e) {
                    ConsoleWriter.logServer("客户端关闭连接");
                    break;
                }
            }

        } catch (java.net.SocketException e) {
            ConsoleWriter.logError("客户端连接异常断开: " + e.getMessage());
        } catch (IOException e) {
            ConsoleWriter.logError("IO异常: " + e.getMessage());
        } catch (Exception e) {
            // 处理其他异常，返回500错误
            ConsoleWriter.logError("服务器内部错误: " + e.getMessage());
            try {
                HttpResponse errorResponse = create500ErrorResponse();
                OutputStream output = socket.getOutputStream();
                output.write(errorResponse.toBytes());
                output.flush();
            } catch (IOException ignored) {
                // 如果连接已断开，无法发送错误响应
            }
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * 创建500内部服务器错误响应
     */
    private HttpResponse create500ErrorResponse() {
        HttpResponse response = new HttpResponse();
        response.setVersion("HTTP/1.1");
        response.setStatusCode(500);
        response.setReasonPhrase("Internal Server Error");
        
        String body = "<html><body><h1>500 Internal Server Error</h1><p>The server encountered an unexpected condition.</p></body></html>";
        response.setBody(body.getBytes());
        response.addHeader("Content-Type", "text/html");
        response.addHeader("Content-Length", String.valueOf(body.length()));
        response.addHeader("Connection", "close");
        
        return response;
    }
}