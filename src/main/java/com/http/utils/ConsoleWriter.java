package com.http.utils;

/**
 * 用于统一管理控制台输出的工具类。
 * 提供带格式前缀的日志记录方法，以区分客户端、服务端和错误信息。
 */
public class ConsoleWriter {

    private static final String SERVER_PREFIX = "[SERVER] ";
    private static final String CLIENT_PREFIX = "[CLIENT] ";
    private static final String ERROR_PREFIX = "[ERROR] ";
    private static final String PROMPT = "> ";

    /**
     * 打印服务端日志信息。
     * @param message 要打印的消息。
     */
    public static void logServer(String message) {
        System.out.println(SERVER_PREFIX + message);
    }

    /**
     * 打印客户端日志信息。
     * @param message 要打印的消息。
     */
    public static void logClient(String message) {
        System.out.println(CLIENT_PREFIX + message);
    }

    /**
     * 打印错误信息到标准错误流。
     * @param message 要打印的错误消息。
     */
    public static void logError(String message) {
        System.err.println(ERROR_PREFIX + message);
    }

    /**
     * 打印客户端的输入提示符，不换行。
     */
    public static void prompt() {
        System.out.print(PROMPT);
    }
}
