package com.http.client;
/**
 * HTTP状态码常量
 */
public class HttpClient {
    // 成功状态码
    public static final int OK = 200;
    // 重定向状态码
    public static final int MOVED_PERMANENTLY = 301;
    public static final int FOUND = 302;
    public static final int NOT_MODIFIED = 304;
    // 客户端错误状态码
    public static final int NOT_FOUND = 404;
    public static final int METHOD_NOT_ALLOWED = 405;
    // 服务器错误状态码
    public static final int INTERNAL_SERVER_ERROR = 500;
    /**
     * 根据状态码获取状态描述
     */
    public static String getReasonPhrase(int statusCode) {
        switch (statusCode) {
            case OK:
                return "OK";
            case MOVED_PERMANENTLY:
                return "Moved Permanently";
            case FOUND:
                return "Found";
            case NOT_MODIFIED:
                return "Not Modified";
            case NOT_FOUND:
                return "Not Found";
            case METHOD_NOT_ALLOWED:
                return "Method Not Allowed";
            case INTERNAL_SERVER_ERROR:
                return "Internal Server Error";
            default:
                return "Unknown";
        }
    }
}
