package com.http.server.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    //用户管理，处理注册登录逻辑（内存存储）
    //信息存储
    //单例模式,确保一个类只有一个实例，并提供一个全局访问点。
    private static UserManager instance;
    private Map<String, String> users = new ConcurrentHashMap<>();

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }
    //注册方法
    public boolean register(String username, String password) {

        if (username == null || username.trim().isEmpty() ||
                password == null || password.length() < 3) {
            return false;
        }

        // 检查用户是否已存在
        if (users.containsKey(username)) {
            return false;
        }

        // 存储用户信息
        users.put(username, password);
        return true;
    }
    //登录方法
    public boolean login(String username, String password) {
        String storedPassword = users.get(username);
        return storedPassword != null && storedPassword.equals(password);
    }
}