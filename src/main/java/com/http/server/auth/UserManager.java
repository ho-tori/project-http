package com.http.server.auth;

import java.io.FileReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class UserManager {
    private static UserManager instance;
    private Map<String, String> users = new ConcurrentHashMap<>();

    // 私有构造方法，读取 JSON 文件初始化 users
    private UserManager(String jsonFilePath) {
        try (FileReader reader = new FileReader(jsonFilePath)) {
            JSONTokener tokener = new JSONTokener(reader);
            JSONObject obj = new JSONObject(tokener);
            JSONArray credentials = obj.getJSONArray("credentials");

            for (int i = 0; i < credentials.length(); i++) {
                JSONObject user = credentials.getJSONObject(i);
                String username = user.getString("username");
                String password = user.getString("password"); // 如果是 hashed，可改成 password_sha256
                users.put(username, password);
            }
        } catch (Exception e) {
            // JSON文件加载失败，使用空的用户列表
        }
    }

    // 单例获取方法（默认路径可传入）
    public static synchronized UserManager getInstance(String jsonFilePath) {
        if (instance == null) {
            instance = new UserManager(jsonFilePath);
        }
        return instance;
    }

    // 注册方法
    public boolean register(String username, String password) {
        if (username == null || username.trim().isEmpty() ||
                password == null || password.length() < 3) {
            return false;
        }
        if (users.containsKey(username)) {
            return false;
        }
        users.put(username, password);
        return true;
    }

    // 登录方法
    public boolean login(String username, String password) {
        String storedPassword = users.get(username);
        return storedPassword != null && storedPassword.equals(password);
    }
}
