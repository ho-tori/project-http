package com.http.client;

import java.io.*;
import java.net.*;
import java.util.*;

public class HttpClient {
    private String host;
    private int port;

    public HttpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // 发送GET请求
    public String sendGet(String path, Map<String, String> headers) throws IOException {
        // 1. 建立Socket连接
        Socket socket = new Socket(host, port);

        // 2. 获取输入输出流
        PrintWriter out = new PrintWriter(socket.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // 3. 构造请求报文
        out.print("GET " + path + " HTTP/1.1\r\n");
        out.print("Host: " + host + "\r\n");

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                out.print(entry.getKey() + ": " + entry.getValue() + "\r\n");
            }
        }

        out.print("Connection: keep-alive\r\n");
        out.print("\r\n");
        out.flush();

        // 4. 读取响应
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line).append("\r\n");
        }

        socket.close();
        return response.toString();
    }

    // 发送POST请求
    public String sendPost(String path, Map<String, String> headers, String body) throws IOException {
        Socket socket = new Socket(host, port);
        PrintWriter out = new PrintWriter(socket.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // 写请求行和头
        out.print("POST " + path + " HTTP/1.1\r\n");
        out.print("Host: " + host + "\r\n");
        out.print("Content-Length: " + body.getBytes().length + "\r\n");
        out.print("Content-Type: application/x-www-form-urlencoded\r\n");

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                out.print(entry.getKey() + ": " + entry.getValue() + "\r\n");
            }
        }

        out.print("\r\n");
        out.print(body);
        out.flush();

        // 读取响应
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line).append("\r\n");
        }

        socket.close();
        return response.toString();
    }

    // 简单重定向处理（301/302）
    public String handleRedirect(String response) throws IOException {
        if (response.startsWith("HTTP/1.1 301") || response.startsWith("HTTP/1.1 302")) {
            int index = response.indexOf("Location:");
            if (index != -1) {
                String location = response.substring(index + 9).split("\r\n")[0].trim();
                System.out.println("🌸重定向到：" + location);
                return sendGet(location, null);
            }
        }
        return response;
    }

    public static void main(String[] args) throws IOException {
        HttpClient client = new HttpClient("localhost", 8080);

        System.out.println("---- 🌸 发送GET请求 ----");
        String res = client.sendGet("/", null);
        System.out.println(res);

        System.out.println("---- 🌸 发送POST请求 ----");
        String body = "username=alice&password=1234";
        String postRes = client.sendPost("/login", null, body);
        System.out.println(postRes);
    }
}
