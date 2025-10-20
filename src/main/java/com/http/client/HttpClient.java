package com.http.client;

import com.http.common.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * HTTP客户端
 */
public class HttpClient {
    private String host;
    private int port;

    public HttpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 发送HTTP请求
     */
    public HttpResponse sendRequest(HttpRequest request) throws IOException {
        Socket socket = new Socket(host, port);

        try (PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // 发送请求
            writer.print(request.toString());
            writer.flush();

            // 读取响应
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            int contentLength = 0;
            boolean headerComplete = false;

            // 读取响应头
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line).append("\r\n");

                if (line.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.split(":")[1].trim());
                }

                if (line.isEmpty()) {
                    headerComplete = true;
                    break;
                }
            }

            // 读取响应体
            if (headerComplete && contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                reader.read(bodyChars, 0, contentLength);
                responseBuilder.append(new String(bodyChars));
            }

            return HttpResponse.parse(responseBuilder.toString());

        } finally {
            socket.close();
        }
    }

    /**
     * 发送GET请求
     */
    public HttpResponse get(String uri) throws IOException {
        HttpRequest request = new HttpRequest("GET", uri);
        request.addHeader("Host", host + ":" + port);
        request.addHeader("User-Agent", "Simple-HTTP-Client/1.0");
        request.addHeader("Connection", "close");

        return sendRequest(request);
    }

    /**
     * 发送POST请求
     */
    public HttpResponse post(String uri, String body) throws IOException {
        HttpRequest request = new HttpRequest("POST", uri);
        request.addHeader("Host", host + ":" + port);
        request.addHeader("User-Agent", "Simple-HTTP-Client/1.0");
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Content-Length", String.valueOf(body.getBytes().length));
        request.addHeader("Connection", "close");
        request.setBody(body);

        return sendRequest(request);
    }

    /**
     * 处理重定向响应
     */
    public HttpResponse handleRedirect(HttpResponse response, int maxRedirects) throws IOException {
        int redirectCount = 0;
        HttpResponse currentResponse = response;

        while (redirectCount < maxRedirects) {
            int statusCode = currentResponse.getStatusCode();

            if (statusCode == HttpStatus.MOVED_PERMANENTLY ||
                    statusCode == HttpStatus.FOUND) {

                String location = currentResponse.getHeader("Location");
                if (location == null) {
                    break;
                }

                System.out.println("重定向到: " + location);

                // 发送新请求到重定向的位置
                HttpRequest redirectRequest = new HttpRequest("GET", location);
                redirectRequest.addHeader("Host", host + ":" + port);
                redirectRequest.addHeader("User-Agent", "Simple-HTTP-Client/1.0");
                redirectRequest.addHeader("Connection", "close");

                currentResponse = sendRequest(redirectRequest);
                redirectCount++;

            } else if (statusCode == HttpStatus.NOT_MODIFIED) {
                System.out.println("资源未修改 (304)");
                break;
            } else {
                break;
            }
        }

        if (redirectCount >= maxRedirects) {
            System.out.println("重定向次数过多，停止重定向");
        }

        return currentResponse;
    }

    /**
     * 显示响应信息
     */
    public void displayResponse(HttpResponse response) {
        System.out.println("=== HTTP响应 ===");
        System.out.println("状态: " + response.getStatusCode() + " " + response.getReasonPhrase());

        System.out.println("\n响应头:");
        for (String headerName : response.getHeaders().keySet()) {
            System.out.println(headerName + ": " + response.getHeader(headerName));
        }

        System.out.println("\n响应体:");
        if (response.getBody() != null) {
            String contentType = response.getHeader("Content-Type");
            if (contentType != null && MimeType.isTextType(contentType)) {
                System.out.println(new String(response.getBody()));
            } else {
                System.out.println("[二进制内容，长度: " + response.getBody().getBytes(StandardCharsets.UTF_8).length + " 字节]");
            }
        } else {
            System.out.println("[无响应体]");
        }
        System.out.println("================");
    }

    /**
     * 命令行界面
     */
    public void startCommandLineInterface() {
        try (Scanner scanner = new Scanner(System.in)) {

            System.out.println("简单HTTP客户端");
            System.out.println("连接到服务器: " + host + ":" + port);
            System.out.println("支持的命令:");
            System.out.println("  GET <uri> - 发送GET请求");
            System.out.println("  POST <uri> <body> - 发送POST请求");
            System.out.println("  REGISTER <username> <password> - 用户注册");
            System.out.println("  LOGIN <username> <password> - 用户登录");
            System.out.println("  QUIT - 退出");
            System.out.println();

            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    continue;
                }

                String[] parts = input.split("\\s+");
                String command = parts[0].toUpperCase();

                try {
                    switch (command) {
                        case "GET":
                            if (parts.length < 2) {
                                System.out.println("用法: GET <uri>");
                                break;
                            }
                            handleGetCommand(parts[1]);
                            break;

                        case "POST":
                            if (parts.length < 3) {
                                System.out.println("用法: POST <uri> <body>");
                                break;
                            }
                            String body = String.join(" ", java.util.Arrays.copyOfRange(parts, 2, parts.length));
                            handlePostCommand(parts[1], body);
                            break;

                        case "REGISTER":
                            if (parts.length < 3) {
                                System.out.println("用法: REGISTER <username> <password>");
                                break;
                            }
                            handleRegisterCommand(parts[1], parts[2]);
                            break;

                        case "LOGIN":
                            if (parts.length < 3) {
                                System.out.println("用法: LOGIN <username> <password>");
                                break;
                            }
                            handleLoginCommand(parts[1], parts[2]);
                            break;

                        case "QUIT":
                            System.out.println("再见！");
                            return;

                        default:
                            System.out.println("未知命令: " + command);
                            break;
                    }
                } catch (IOException e) {
                    System.out.println("请求失败: " + e.getMessage());
                }
            }
        }
    }

    private void handleGetCommand(String uri) throws IOException {
        HttpResponse response = get(uri);
        response = handleRedirect(response, 5);
        displayResponse(response);
    }

    private void handlePostCommand(String uri, String body) throws IOException {
        HttpResponse response = post(uri, body);
        displayResponse(response);
    }

    private void handleRegisterCommand(String username, String password) throws IOException {
        String body = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";
        HttpResponse response = post("/api/register", body);
        displayResponse(response);
    }

    private void handleLoginCommand(String username, String password) throws IOException {
        String body = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";
        HttpResponse response = post("/api/login", body);
        displayResponse(response);
    }

    public static void main(String[] args) {
        HttpClient client = new HttpClient("localhost", 8080);
        client.startCommandLineInterface();
    }
}