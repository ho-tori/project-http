package com.http.server.handler;

import com.http.common.HttpRequest;
import com.http.common.HttpResponse;
import com.http.common.HttpStatus;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FileUploadHandler {
    
    private final String uploadDir;
    
    public FileUploadHandler() {
        this.uploadDir = "src/main/java/web/uploads/";
        // 确保上传目录存在
        new File(uploadDir).mkdirs();
    }
    
    /**
     * 处理文件上传，支持POST请求
     */
    public HttpResponse handle(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        response.setVersion("HTTP/1.1");
        
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
            response.setReasonPhrase(HttpStatus.getReasonPhrase(HttpStatus.METHOD_NOT_ALLOWED));
            response.setBody("405 Method Not Allowed - Only POST supported");
            response.addHeader("Content-Type", "text/plain");
            return response;
        }
        
        try {
            byte[] body = request.getBody();
            if (body == null || body.length == 0) {
                return buildBadRequest("No file data received");
            }
            
            // 根据请求内容确定文件扩展名
            String fileExtension = determineFileExtension(request, body);
            String filename = "uploaded_" + System.currentTimeMillis() + fileExtension;
            File uploadFile = new File(uploadDir + filename);
            
            try (FileOutputStream fos = new FileOutputStream(uploadFile)) {
                fos.write(body);
            }
            
            // 成功响应
            String responseBody = "{\"status\":\"success\",\"message\":\"File uploaded successfully\",\"filename\":\"" + filename + "\",\"size\":" + body.length + "}";
            response.setStatusCode(HttpStatus.OK);
            response.setReasonPhrase(HttpStatus.getReasonPhrase(HttpStatus.OK));
            response.setBody(responseBody.getBytes(StandardCharsets.UTF_8));
            response.addHeader("Content-Type", "application/json; charset=utf-8");
            response.addHeader("Content-Length", String.valueOf(responseBody.length()));
            response.addHeader("Connection", "close");
            
            return response;
            
        } catch (IOException e) {
            return buildInternalError("Failed to save uploaded file: " + e.getMessage());
        } catch (Exception e) {
            return buildInternalError("Unexpected error: " + e.getMessage());
        }
    }
    
    private HttpResponse buildBadRequest(String message) {
        HttpResponse response = new HttpResponse();
        response.setVersion("HTTP/1.1");
        response.setStatusCode(400);
        response.setReasonPhrase("Bad Request");
        
        String body = "{\"status\":\"error\",\"message\":\"" + message + "\"}";
        response.setBody(body.getBytes(StandardCharsets.UTF_8));
        response.addHeader("Content-Type", "application/json; charset=utf-8");
        response.addHeader("Content-Length", String.valueOf(body.length()));
        response.addHeader("Connection", "close");
        
        return response;
    }
    
    private HttpResponse buildInternalError(String message) {
        HttpResponse response = new HttpResponse();
        response.setVersion("HTTP/1.1");
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        response.setReasonPhrase(HttpStatus.getReasonPhrase(HttpStatus.INTERNAL_SERVER_ERROR));
        
        String body = "{\"status\":\"error\",\"message\":\"" + message + "\"}";
        response.setBody(body.getBytes(StandardCharsets.UTF_8));
        response.addHeader("Content-Type", "application/json; charset=utf-8");
        response.addHeader("Content-Length", String.valueOf(body.length()));
        response.addHeader("Connection", "close");
        
        return response;
    }
    
    /**
     * 根据请求和内容确定文件扩展名
     */
    private String determineFileExtension(HttpRequest request, byte[] body) {
        // 优先使用文件内容检测（Magic Numbers）- 最可靠的方法
        if (body.length >= 4) {
            // JPEG文件头检测
            if (body[0] == (byte)0xFF && body[1] == (byte)0xD8) {
                return ".jpg";
            }
            // PNG文件头检测  
            else if (body[0] == (byte)0x89 && body[1] == 0x50 && 
                      body[2] == 0x4E && body[3] == 0x47) {
                return ".png";
            }
        }
        
        // 然后检查Content-Type头（仅对明确的图片类型）
        String contentType = request.getHeaders().get("Content-Type");
        if (contentType != null) {
            if (contentType.contains("image/jpeg") || contentType.contains("image/jpg")) {
                return ".jpg";
            } else if (contentType.contains("image/png")) {
                return ".png";
            }
        }
        
        // 最后检查是否为文本内容
        if (isTextContent(body)) {
            return ".txt";
        }
        
        // 默认为二进制文件
        return ".bin";
    }
    
    /**
     * 检查内容是否为文本类型
     */
    private boolean isTextContent(byte[] body) {
        if (body.length == 0) return true;
        
        int textChars = 0;
        int totalChars = Math.min(body.length, 1024); // 只检查前1024字节
        
        for (int i = 0; i < totalChars; i++) {
            byte b = body[i];
            // ASCII可打印字符 + 常见控制字符
            if ((b >= 32 && b <= 126) || b == 9 || b == 10 || b == 13) {
                textChars++;
            } else if (b < 0) {
                // 负数字节可能是UTF-8编码
                textChars++;
            }
        }
        
        // 如果80%以上是文本字符，认为是文本文件
        return (double) textChars / totalChars > 0.8;
    }
}