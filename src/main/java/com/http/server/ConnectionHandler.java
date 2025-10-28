package com.http.server;

import java.io.*;
import java.net.*;
import com.http.common.HttpRequest;
import com.http.common.HttpResponse;

public class ConnectionHandler implements Runnable{
    //每个客户端请求的“单独处理线程”
    //实现接口更灵活，可以继承别的类

    //①	从 socket 读取请求报文	使用 HttpRequest(InputStream) 解析
    //②	打印或理解请求信息	（调试或日志）
    //③	构造 HTTP 响应	使用 HttpResponse
    //④	发送响应回客户端	输出字节流并关闭 socket

    private Socket socket;

    public ConnectionHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run(){
        try (
                InputStream input = socket.getInputStream();
                OutputStream output = socket.getOutputStream()
        ) {
            // 1️⃣ 解析请求
            HttpRequest request = HttpRequest.parse(input.toString());
            String uri = request.getUri();
            System.out.println("📩 收到请求: " + request.getMethod() + " " + uri);

            // 2️⃣ 目前仅GET请求，构造响应
            HttpResponse response = new HttpResponse();
            response.setVersion("HTTP/1.1");
            response.setStatusCode(200);
            response.setReasonPhrase("OK");
            response.addHeader("Content-Type", "text/html; charset=UTF-8");
            response.setBody("<h1>Hello, " + request.getUri() + "</h1>");

            // 3️⃣ 发送响应
            output.write(response.toBytes());//方法待添加
            output.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
