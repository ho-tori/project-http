package com.http.server;

import com.http.utils.ConsoleWriter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

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

        ConsoleWriter.logServer("💫 HTTP服务器已启动，监听端口: " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();// 等待客户端连接
                
                // 设置Socket超时时间支持长连接 - 临时增加到2分钟用于调试
                clientSocket.setSoTimeout(120000); // 120秒超时
                
                ConsoleWriter.logServer("🔗 收到客户端连接: " + clientSocket.getInetAddress());
                //处理连接
                new Thread(new ConnectionHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            ConsoleWriter.logError("服务器异常: " + e.getMessage());
        }

    }

    public static void main(String[] args) {
        //启动HTTP服务器
        HttpServer server = new HttpServer(6175);//port可以改
        server.start();
    }
}
