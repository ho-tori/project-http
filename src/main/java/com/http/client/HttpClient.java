package com.http.client;

import com.http.common.*;
import com.http.utils.ConsoleWriter;

import java.io.*;
import java.net.*;
import java.util.Scanner;
// 移除未使用的导入
import java.util.Map;

/**
 * HTTP客户端
 */
public class HttpClient {
    private String host;
    private int port;
    // 复用同一个 Socket 以支持长连接
    private Socket persistentSocket;
    // 全局开关：是否启用长连接（默认开启）
    private boolean enableKeepAlive = true;
    // 简易缓存：记录每个 URI 的 Last-Modified
    private final java.util.Map<String, String> lastModifiedCache = new java.util.HashMap<>();

    public HttpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 发送HTTP请求
     */
    public HttpResponse sendRequest(HttpRequest request) throws IOException {
        // 若没有连接或已关闭，建立一次新的连接
        if (enableKeepAlive && (persistentSocket == null || persistentSocket.isClosed())) {
            persistentSocket = new Socket(host, port);
            // 设置读取超时，避免服务端长时间不返回导致阻塞
            try { persistentSocket.setSoTimeout(30_000); } catch (SocketException ignored) {}
        }
        // 根据开关决定使用持久连接还是临时连接
        Socket socketToUse = enableKeepAlive ? persistentSocket : new Socket(host, port);
        OutputStream out = socketToUse.getOutputStream();
        InputStream in = socketToUse.getInputStream();

            // 发送请求
            StringBuilder headerBuilder = new StringBuilder();
            headerBuilder.append(request.getMethod()).append(" ")
                    .append(request.getUri()).append(" ")
                    .append(request.getVersion()).append("\r\n");

            for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                headerBuilder.append(entry.getKey()).append(": ")
                        .append(entry.getValue()).append("\r\n");
            }
            headerBuilder.append("\r\n");

            out.write(headerBuilder.toString().getBytes("UTF-8"));
            if (request.getBody() != null) out.write(request.getBody());
            out.flush();

            // 直接解析响应（不要提前读取）
            HttpResponse resp = HttpResponse.parse(in);

            // 如果服务端指示关闭，则本端也关闭连接
            String conn = resp.getHeader("Connection");
            if (enableKeepAlive && conn != null && conn.equalsIgnoreCase("close")) {
                try { if (persistentSocket != null) persistentSocket.close(); } catch (IOException ignored) {}
                persistentSocket = null;
            }

            // 若不开启长连接，则每次请求完立即关闭临时连接
            if (!enableKeepAlive) {
                try { socketToUse.close(); } catch (IOException ignored) {}
            }
            return resp;
    }

    /**
     * 发送GET请求
     */
    public HttpResponse get(String uri) throws IOException {
        HttpRequest request = new HttpRequest("GET", uri);
        request.addHeader("Host", host + ":" + port);
        request.addHeader("User-Agent", "Simple-HTTP-Client/1.0");
        request.addHeader("Connection", enableKeepAlive ? "keep-alive" : "close");
        // 默认：若有缓存则携带 If-Modified-Since
        String cached = lastModifiedCache.get(normalizeUri(uri));
        if (cached != null) {
            request.addHeader("If-Modified-Since", cached);
        }
        HttpResponse resp = sendRequest(request);
        // 收到 200 刷新缓存；304 保留旧缓存
        if (resp.getStatusCode() == HttpStatus.OK) {
            String lm = resp.getHeader("Last-Modified");
            if (lm != null) {
                lastModifiedCache.put(normalizeUri(uri), lm);
            }
        }
        return resp;
    }


    private String normalizeUri(String uri) {
        if (uri == null) return "/";
        int q = uri.indexOf('?');
        if (q >= 0) uri = uri.substring(0, q);
        if (uri.isEmpty()) uri = "/";
        return uri;
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
        request.addHeader("Connection", enableKeepAlive ? "keep-alive" : "close");
        request.setBody(body);

        return sendRequest(request);
    }

    /**
     * 发送二进制POST（指定 Content-Type）
     */
    public HttpResponse postBinary(String uri, byte[] body, String contentType) throws IOException {
        if (contentType == null || contentType.trim().isEmpty()) {
            contentType = "application/octet-stream";
        }
        HttpRequest request = new HttpRequest("POST", uri);
        request.addHeader("Host", host + ":" + port);
        request.addHeader("User-Agent", "Simple-HTTP-Client/1.0");
        request.addHeader("Content-Type", contentType);
        request.addHeader("Content-Length", String.valueOf(body.length));
        request.addHeader("Connection", enableKeepAlive ? "keep-alive" : "close");
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
                redirectRequest.addHeader("Connection", enableKeepAlive ? "keep-alive" : "close");

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
            ConsoleWriter.logClient("  KEEPALIVE <on|off>               - 开启/关闭长连接");
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
                            String contentTypeForPost = null;

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

                                    // 根据扩展名推断 Content-Type
                                    String fname = file.getName().toLowerCase();
                                    if (fname.endsWith(".png")) contentTypeForPost = "image/png";
                                    else if (fname.endsWith(".jpg") || fname.endsWith(".jpeg")) contentTypeForPost = "image/jpeg";
                                    else if (fname.endsWith(".html") || fname.endsWith(".htm")) contentTypeForPost = "text/html";
                                    else if (fname.endsWith(".txt")) contentTypeForPost = "text/plain";
                                    else if (fname.endsWith(".json")) contentTypeForPost = "application/json";
                                    else contentTypeForPost = "application/octet-stream";
                                } catch (Exception e) {
                                    ConsoleWriter.logError("读取文件失败: " + e.getMessage());
                                    break;
                                }
                            } else {
                                // 🌸 普通文本 POST
                                bodyBytes = bodyInput.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                                ConsoleWriter.logClient("🌸 使用文本 POST 请求: " + bodyInput);
                                contentTypeForPost = "text/plain";
                            }

                            // 根据内容类型选择POST方法
                            HttpResponse resp;
                            if ("application/json".equals(contentTypeForPost)) {
                                resp = post(uri, bodyBytes);
                            } else {
                                resp = postBinary(uri, bodyBytes, contentTypeForPost);
                            }
                            displayResponse(resp);
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
                            // 退出时关闭持久连接
                            if (persistentSocket != null && !persistentSocket.isClosed()) {
                                try { persistentSocket.close(); } catch (IOException ignored) {}
                                persistentSocket = null;
                            }
                            return;

                        case "KEEPALIVE":
                            if (parts.length < 2) {
                                ConsoleWriter.logError("用法: KEEPALIVE <on|off>");
                                break;
                            }
                            String opt = parts[1].toLowerCase();
                            if ("on".equals(opt)) {
                                enableKeepAlive = true;
                                ConsoleWriter.logClient("已开启长连接模式");
                            } else if ("off".equals(opt)) {
                                enableKeepAlive = false;
                                ConsoleWriter.logClient("已关闭长连接模式（每次请求独立连接）");
                                if (persistentSocket != null && !persistentSocket.isClosed()) {
                                    try { persistentSocket.close(); } catch (IOException ignored) {}
                                    persistentSocket = null;
                                }
                            } else {
                                ConsoleWriter.logError("参数错误，应为 on 或 off");
                            }
                            break;

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

        displayResponse(response); // 先打印响应信息

        // 判断是否是二进制内容（图片/文件）
        String contentType = response.getHeader("Content-Type");
        if (contentType != null && !MimeType.isTextType(contentType)) {
            byte[] body = response.getBody();
            if (body != null && body.length > 0) {
                // 根据 URI 和 Content-Type 生成文件名
                String filename = generateFileName(uri, contentType);
                saveBinaryFile(body, filename);
            }
        }
    }

    // 保存文件方法
    private void saveBinaryFile(byte[] data, String fileName) {
        File dir = new File("downloads/");
        if (!dir.exists()) dir.mkdirs(); // 确保目录存在

        File file = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            System.out.println("文件已保存: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("保存文件失败: " + e.getMessage());
        }
    }

    // 根据 URI 或 Content-Type 自动生成文件名
    private String generateFileName(String uri, String contentType) {
        // 1) 规范化 Content-Type（去掉参数部分，例如 charset）
        String normalized = contentType == null ? "application/octet-stream" : contentType.split(";", 2)[0].trim().toLowerCase();

        // 2) MIME → 扩展名映射（与服务端 MimeType 保持一致并补充常用类型）
        java.util.Map<String, String> mimeToExt = new java.util.HashMap<>();
        mimeToExt.put("text/html", "html");
        mimeToExt.put("text/plain", "txt");
        mimeToExt.put("image/png", "png");
        mimeToExt.put("image/jpeg", "jpg");
        mimeToExt.put("application/json", "json");
        mimeToExt.put("application/xml", "xml");
        mimeToExt.put("application/octet-stream", "bin");

        String ext = mimeToExt.getOrDefault(normalized, "bin");

        // 3) 从 URI 提取文件名（含扩展名），若没有则生成
        String raw = uri;
        int q = raw.indexOf('?');
        if (q >= 0) raw = raw.substring(0, q);
        String name = raw.substring(raw.lastIndexOf('/') + 1);

        if (name.isEmpty()) {
            name = "downloaded_" + System.currentTimeMillis() + "." + ext;
        } else {
            // 如果原始名没有扩展名，补充一个
            int dot = name.lastIndexOf('.');
            if (dot < 0 || dot == name.length() - 1) {
                name = name + "." + ext;
            }
        }
        return name;
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

    public static void main(String[] args) throws UnknownHostException {
        HttpClient client = new HttpClient("127.0.0.1", 6175);
        InetAddress localHost = InetAddress.getLocalHost();
        System.out.println("本机 IP: " + localHost.getHostAddress());
        client.startCommandLineInterface();
    }

    // 允许外部（如 GUI）动态切换是否启用长连接
    public void setEnableKeepAlive(boolean enable) {
        this.enableKeepAlive = enable;
        if (!enable && persistentSocket != null && !persistentSocket.isClosed()) {
            try { persistentSocket.close(); } catch (IOException ignored) {}
            persistentSocket = null;
        }
    }
}