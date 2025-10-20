package com.http.common;

import java.util.HashMap;
import java.util.Map;

public class MimeType {
    //支持多种MIME类型（文本、图片等）
    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    //static静态代码块：在类加载时自动执行，用于初始化数据
    //相当于一部字典，把文件扩展名映射到对应的MIME类型
    static {
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("txt", "text/plain");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
    }
    public static String getMimeType(String fileName) {
        // 找到文件名中最后一个点的位置
        int lastDotIndex = fileName.lastIndexOf(".");

        // 如果没有点或者点在最后，返回默认类型
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "application/octet-stream"; // 默认的二进制文件类型
        }

        // 提取扩展名（点后面的部分）
        String extension = fileName.substring(lastDotIndex + 1).toLowerCase();

        // 从Map中查找对应的MIME类型，如果找不到就用默认类型
        return MIME_TYPES.getOrDefault(extension, "application/octet-stream");
    }
    public static boolean isTextType(String mimeType) {
        return mimeType.startsWith("text/");
    }
//    public static void main(String[] args) {
//        System.out.println(getMimeType("index.html")); // 输出: text/html
//        System.out.println(getMimeType("photo.jpg"));  // 输出: image/jpeg
//        System.out.println(getMimeType("test.txt"));   // 输出: text/plain
//        System.out.println(getMimeType("unknown.xyz")); // 输出: application/octet-stream
//    }
}
