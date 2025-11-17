package com.http.server;

import java.io.*;
import java.net.*;
import com.http.common.HttpRequest;
import com.http.common.HttpResponse;
import com.http.common.HttpStatus;
import com.http.common.MimeType;
import com.http.server.handler.LoginHandler;
import com.http.server.handler.ReigisterHandler;
import com.http.server.handler.StaticFileHandler;

public class ConnectionHandler implements Runnable{
    //每个客户端请求的“单独处理线程”
    //实现接口更灵活，可以继承别的类

    //①	从 socket 读取请求报文	使用 HttpRequest(InputStream) 解析
    //②	打印或理解请求信息	（调试或日志）
    //③	构造 HTTP 响应	使用 HttpResponse
    //④	发送响应回客户端	输出字节流并关闭 socket

    private Socket socket;
    private final LoginHandler loginHandler = new LoginHandler();
    private final ReigisterHandler registerHandler = new ReigisterHandler();
    private final StaticFileHandler staticFileHandler = new StaticFileHandler();

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
            HttpRequest request = new HttpRequest(input);
            if(request == null){
                System.out.println("⚠️ 无法解析请求");
                //sendBadRequest(output);
                return;
            }
            String uri = request.getUri();
            System.out.println("📩 收到请求: " + request.getMethod() + " " + uri);

            // 2️⃣ 构造响应
            HttpResponse response = handleRequest(request);

            // 3️⃣ 发送响应
            output.write(response.toBytes());//方法待添加
            output.flush();

            // 4️⃣ 判断是否关闭连接  现在还没有getHeader方法???
/*            if (!"keep-alive".equalsIgnoreCase(request.getHeader("Connection"))) {
                socket.close();
            }*/

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private  HttpResponse handleRequest(HttpRequest request){
        HttpResponse response = new HttpResponse();
        try{
            String method = request.getMethod();
            String uri = request.getUri();

            if("GET".equalsIgnoreCase(method)){
                return handleGet(uri);
            }else if("POST".equalsIgnoreCase(method)){
                return hanlePost(uri,request.getBody());
            }else{
                //other methods
            }
        }catch (Exception e){
            response.setStatusCode(500);
            response.setReasonPhrase("Internal Server Error");
            response.setBody("<h1>500 Internal Server Error</h1>");
        }
        return response;

    }

    private  HttpResponse handleGet(String uri){
        HttpResponse response = new HttpResponse();
        File file = new File(uri);

        if(file.exists() && file.isFile()){
            response.setStatusCode(200);
            response.setReasonPhrase("OK");
            response.setBody(file);//?
            response.setHeaders("Content-Type", getMimeType(file));
        }
    }

    /*private void sendBadRequest(OutputStream output) throws IOException {
        HttpResponse response = new HttpResponse();
        response.setVersion("HTTP/1.1");
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.setReasonPhrase(HttpStatus.getReasonPhrase(HttpStatus.BAD_REQUEST));
        response.setBody("400 Bad Request");
        output.write(response.toBytes());
        output.flush();
    }*/

}
