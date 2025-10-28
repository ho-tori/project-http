package com.http.server;

import com.http.common.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {
    //HTTP服务器主类，监听端口并处理连接
    private int port;//监听端口 一定private吗?

    public HttpServer(int port) {
        this.port = port;
    }

    public void start() {
        //启动服务器，监听端口，处理连接
        System.out.println(" 💫 HTTP服务器已启动，监听端口: " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();// 等待客户端连接
                System.out.println("🔗 收到客户端连接: " + clientSocket.getInetAddress());
                //处理连接
                new Thread(new ConnectionHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        //启动HTTP服务器
        HttpServer server = new HttpServer(6175);//port可以改
        server.start();
    }
}
