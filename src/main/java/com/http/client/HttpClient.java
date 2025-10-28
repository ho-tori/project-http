package com.http.client;

import com.http.common.*;
import com.http.utils.ConsoleWriter;

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
    public HttpResponse post(String uri, byte[] body) throws IOException {
        HttpRequest request = new HttpRequest("POST", uri);
        request.addHeader("Host", host + ":" + port);
        request.addHeader("User-Agent", "Simple-HTTP-Client/1.0");
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Content-Length", String.valueOf(body.length));
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

                ConsoleWriter.logClient("重定向到: " + location);

                // 发送新请求到重定向的位置
                HttpRequest redirectRequest = new HttpRequest("GET", location);
                redirectRequest.addHeader("Host", host + ":" + port);
                redirectRequest.addHeader("User-Agent", "Simple-HTTP-Client/1.0");
                redirectRequest.addHeader("Connection", "close");

                currentResponse = sendRequest(redirectRequest);
                redirectCount++;

            } else if (statusCode == HttpStatus.NOT_MODIFIED) {
                ConsoleWriter.logClient("资源未修改 (304)");
                break;
            } else {
                break;
            }
        }

        if (redirectCount >= maxRedirects) {
            ConsoleWriter.logError("重定向次数过多，停止重定向");
        }

        return currentResponse;
    }

    /**
     * 显示响应信息
     */
    public void displayResponse(HttpResponse response) {
        ConsoleWriter.logClient("=== HTTP响应 ===");
        ConsoleWriter.logClient("状态: " + response.getStatusCode() + " " + response.getReasonPhrase());

        ConsoleWriter.logClient("\n响应头:");
        for (String headerName : response.getHeaders().keySet()) {
            ConsoleWriter.logClient(headerName + ": " + response.getHeader(headerName));
        }

        ConsoleWriter.logClient("\n响应体:");
        if (response.getBody() != null) {
            String contentType = response.getHeader("Content-Type");
            if (contentType != null && MimeType.isTextType(contentType)) {
                ConsoleWriter.logClient(new String(response.getBody()));
            } else {
                ConsoleWriter.logClient("[二进制内容，长度: " + response.getBody().length + " 字节]");
            }
        } else {
            ConsoleWriter.logClient("[无响应体]");
        }
        ConsoleWriter.logClient("================");
    }

    /**
     * 命令行界面
     */
    public void startCommandLineInterface() {
        try (Scanner scanner = new Scanner(System.in)) {

            ConsoleWriter.logClient("简单HTTP客户端");
            ConsoleWriter.logClient("连接到服务器: " + host + ":" + port);
            ConsoleWriter.logClient("支持的命令:");
            ConsoleWriter.logClient("  GET <uri>                        - 发送GET请求");
            ConsoleWriter.logClient("  POST <uri> <text|file_path>      - 发送POST请求，可直接发送文本或上传文件");
            ConsoleWriter.logClient("     示例:");
            ConsoleWriter.logClient("        POST /api/upload hello=world      (发送文本数据)");
            ConsoleWriter.logClient("        POST /api/upload ./data/test.txt  (上传文件)");
            ConsoleWriter.logClient("  REGISTER <username> <password>   - 用户注册");
            ConsoleWriter.logClient("  LOGIN <username> <password>      - 用户登录");
            ConsoleWriter.logClient("  QUIT                             - 退出客户端");
            ConsoleWriter.logClient(""); // 打印一个空行

            while (true) {
                ConsoleWriter.prompt();
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
                                ConsoleWriter.logError("用法: GET <uri>");
                                break;
                            }
                            handleGetCommand(parts[1]);
                            break;

                        case "POST":
                            if (parts.length < 3) {
                                ConsoleWriter.logError("用法: POST <uri> <body或文件路径>");
                                break;
                            }

                            String uri = parts[1];
                            String bodyInput = String.join(" ", java.util.Arrays.copyOfRange(parts, 2, parts.length));
                            byte[] bodyBytes = null;

                            java.io.File file = new java.io.File(bodyInput);
                            if (file.exists() && file.isFile()) {
                                // 🌸 文件上传模式
                                try (java.io.FileInputStream fis = new java.io.FileInputStream(file);
                                        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream()) {

                                    byte[] tmp = new byte[4096];
                                    int len;
                                    while ((len = fis.read(tmp)) != -1) {
                                        buffer.write(tmp, 0, len);
                                    }
                                    bodyBytes = buffer.toByteArray();
                                    ConsoleWriter.logClient("🌸 检测到文件上传: " + file.getName() + " (" + bodyBytes.length + " bytes)");
                                } catch (Exception e) {
                                    ConsoleWriter.logError("读取文件失败: " + e.getMessage());
                                    break;
                                }
                            } else {
                                // 🌸 普通文本 POST
                                bodyBytes = bodyInput.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                                ConsoleWriter.logClient("🌸 使用文本 POST 请求: " + bodyInput);
                            }

                            handlePostCommand(uri, bodyBytes);
                            break;

                        case "REGISTER":
                            if (parts.length < 3) {
                                ConsoleWriter.logError("用法: REGISTER <username> <password>");
                                break;
                            }
                            handleRegisterCommand(parts[1], parts[2]);
                            break;

                        case "LOGIN":
                            if (parts.length < 3) {
                                ConsoleWriter.logError("用法: LOGIN <username> <password>");
                                break;
                            }
                            handleLoginCommand(parts[1], parts[2]);
                            break;

                        case "QUIT":
                            ConsoleWriter.logClient("再见！");
                            return;

                        default:
                            ConsoleWriter.logError("未知命令: " + command);
                            break;
                    }
                } catch (IOException e) {
                    ConsoleWriter.logError("请求失败: " + e.getMessage());
                }
            }
        }
    }

    private void handleGetCommand(String uri) throws IOException {
        HttpResponse response = get(uri);
        response = handleRedirect(response, 5);
        displayResponse(response);
    }

    private void handlePostCommand(String uri, byte[] body) throws IOException {
        HttpResponse response = post(uri, body);
        displayResponse(response);
    }

    private void handleRegisterCommand(String username, String password) throws IOException {
        String body = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";
        HttpResponse response = post("/api/register", body.getBytes());
        displayResponse(response);
    }

    private void handleLoginCommand(String username, String password) throws IOException {
        String body = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";
        HttpResponse response = post("/api/login", body.getBytes());
        displayResponse(response);
    }

    public static void main(String[] args) {
        HttpClient client = new HttpClient("localhost", 6175);
        client.startCommandLineInterface();
    }
}