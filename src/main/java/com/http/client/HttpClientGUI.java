package com.http.client;

import com.http.common.HttpResponse;
import com.http.common.MimeType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;

/**
 * 图形化 HTTP 客户端界面
 *
 * 说明：
 * - 复用现有的 {@link HttpClient} 负责与服务器通信
 * - 支持：
 *   - GET 请求
 *   - POST 文本请求
 *   - 选择文件并以 POST 方式上传
 *   - REGISTER / LOGIN（直接调用对应 API）
 * - 响应会显示在下方文本区域，若为二进制内容则保存到 downloads/ 目录
 */
public class HttpClientGUI extends JFrame {

    // 连接信息
    private JTextField hostField;
    private JTextField portField;
    private JCheckBox keepAliveBox;
    // 复用同一个客户端以便复用底层 Socket
    private HttpClient sharedClient;
    private String lastHost;
    private int lastPort;

    // 通用请求信息
    private JTextField uriField;
    private JTextArea requestBodyArea;

    // 认证信息
    private JTextField usernameField;
    private JPasswordField passwordField;

    // 文件上传
    private JTextField filePathField;

    // 响应显示
    private JTextArea responseArea;
    private JLabel statusLabel;

    public HttpClientGUI() {
        setTitle("简约淡紫 HTTP 客户端 ♡");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 720);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        // 统一设置全局字体（偏可爱一点的圆润字体，如果系统没有会自动回退）
        Font uiFont = new Font("Microsoft YaHei UI", Font.PLAIN, 14);
        setFontRecursively(this.getContentPane(), uiFont);

        // 配色：淡紫色主题
        Color bgMain = new Color(245, 240, 252);      // 主背景淡紫
        Color bgCard = new Color(252, 248, 255);      // 卡片背景更浅
        Color accent = new Color(186, 173, 255);      // 按钮/边框高亮
        Color accentDark = new Color(141, 106, 214);  // 鼠标悬停/深色
        Color textMain = new Color(60, 50, 90);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));
        content.setBackground(bgMain);
        setContentPane(content);

        // ===== 顶部：连接配置 =====
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.setOpaque(true);
        topPanel.setBackground(bgCard);
        topPanel.setBorder(BorderFactory.createTitledBorder("连接设置 ✨"));

        JLabel hostLabel = new JLabel("Host:");
        hostLabel.setForeground(textMain);
        topPanel.add(hostLabel);

        hostField = new JTextField("localhost", 12);
        topPanel.add(hostField);

        JLabel portLabel = new JLabel("Port:");
        portLabel.setForeground(textMain);
        topPanel.add(portLabel);

        portField = new JTextField("6175", 5);
        topPanel.add(portField);

        JButton connectTestBtn = createCuteButton("测试连接 (GET /) ♡", accent, accentDark, Color.WHITE);
        connectTestBtn.addActionListener(this::onTestConnection);
        topPanel.add(connectTestBtn);

        keepAliveBox = new JCheckBox("长连接 (keep-alive)", true);
        keepAliveBox.setBackground(bgCard);
        keepAliveBox.setForeground(textMain);
        topPanel.add(keepAliveBox);

        content.add(topPanel, BorderLayout.NORTH);

        // ===== 中部：请求设置 + 认证 + 文件上传 + 操作按钮 =====
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(bgMain);

        // --- URI 行 ---
        JPanel uriPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        uriPanel.setOpaque(true);
        uriPanel.setBackground(bgCard);
        JLabel uriLabel = new JLabel("请求 URI:");
        uriLabel.setForeground(textMain);
        uriPanel.add(uriLabel);
        uriField = new JTextField("/index.html", 40);
        uriPanel.add(uriField);
        centerPanel.add(uriPanel);

        // --- 请求体 ---
        JPanel bodyPanel = new JPanel(new BorderLayout(5, 5));
        bodyPanel.setBackground(bgCard);
        bodyPanel.setBorder(BorderFactory.createTitledBorder("请求体 (用于 POST 文本) 📝"));
        requestBodyArea = new JTextArea(5, 50);
        requestBodyArea.setLineWrap(true);
        requestBodyArea.setWrapStyleWord(true);
        requestBodyArea.setBackground(Color.WHITE);
        bodyPanel.add(new JScrollPane(requestBodyArea), BorderLayout.CENTER);
        centerPanel.add(bodyPanel);

        // --- 认证区域 ---
        JPanel authPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        authPanel.setOpaque(true);
        authPanel.setBackground(bgCard);
        authPanel.setBorder(BorderFactory.createTitledBorder("用户注册 / 登录 🐱"));
        JLabel userLabel = new JLabel("用户名:");
        userLabel.setForeground(textMain);
        authPanel.add(userLabel);
        usernameField = new JTextField(10);
        authPanel.add(usernameField);
        JLabel pwdLabel = new JLabel("密码:");
        pwdLabel.setForeground(textMain);
        authPanel.add(pwdLabel);
        passwordField = new JPasswordField(10);
        authPanel.add(passwordField);

        JButton registerBtn = createCuteButton("REGISTER ♡", accent, accentDark, Color.WHITE);
        registerBtn.addActionListener(this::onRegister);
        authPanel.add(registerBtn);

        JButton loginBtn = createCuteButton("LOGIN ✧", accent, accentDark, Color.WHITE);
        loginBtn.addActionListener(this::onLogin);
        authPanel.add(loginBtn);

        centerPanel.add(authPanel);

        // --- 文件上传区域 ---
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filePanel.setOpaque(true);
        filePanel.setBackground(bgCard);
        filePanel.setBorder(BorderFactory.createTitledBorder("文件上传 (POST) 🎀"));
        JLabel fileLabel = new JLabel("文件:");
        fileLabel.setForeground(textMain);
        filePanel.add(fileLabel);
        filePathField = new JTextField(30);
        filePanel.add(filePathField);
        JButton chooseFileBtn = createCuteButton("选择文件…", accent, accentDark, Color.WHITE);
        chooseFileBtn.addActionListener(this::onChooseFile);
        filePanel.add(chooseFileBtn);

        JButton uploadBtn = createCuteButton("上传文件到当前 URI ✿", accent, accentDark, Color.WHITE);
        uploadBtn.addActionListener(this::onUploadFile);
        filePanel.add(uploadBtn);

        centerPanel.add(filePanel);

        // --- 通用操作按钮 ---
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        actionPanel.setOpaque(true);
        actionPanel.setBackground(bgMain);
        JButton getBtn = createCuteButton("GET", accent, accentDark, Color.WHITE);
        getBtn.addActionListener(this::onGet);
        actionPanel.add(getBtn);

        JButton postTextBtn = createCuteButton("POST 文本", accent, accentDark, Color.WHITE);
        postTextBtn.addActionListener(this::onPostText);
        actionPanel.add(postTextBtn);

        centerPanel.add(actionPanel);

        content.add(centerPanel, BorderLayout.CENTER);

        // ===== 底部：响应与状态 =====
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBackground(bgMain);

        responseArea = new JTextArea();
        responseArea.setEditable(false);
        // 响应体区域启用自动换行，避免撑大窗口
        responseArea.setLineWrap(true);
        responseArea.setWrapStyleWord(true);
        responseArea.setBackground(Color.WHITE);
        JScrollPane responseScrollPane = new JScrollPane(responseArea);
        // 固定一个相对合适的高度，让上面的输入区域始终可见
        responseScrollPane.setPreferredSize(new Dimension(100, 260));
        responseScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        bottomPanel.add(responseScrollPane, BorderLayout.CENTER);

        statusLabel = new JLabel(" 就绪 ✨");
        statusLabel.setForeground(textMain);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        content.add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 递归设置容器中的字体（让整体更统一 & 可爱）
     */
    private void setFontRecursively(Container container, Font font) {
        for (Component comp : container.getComponents()) {
            comp.setFont(font);
            if (comp instanceof Container) {
                setFontRecursively((Container) comp, font);
            }
        }
    }

    /**
     * 生成淡紫色系按钮，带简单的 hover 效果
     */
    private JButton createCuteButton(String text, Color bg, Color bgHover, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(bgHover.darker(), 1, true));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addChangeListener(e -> {
            ButtonModel model = btn.getModel();
            if (model.isRollover()) {
                btn.setBackground(bgHover);
            } else {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    // ========================= 事件处理 =========================

    private HttpClient createClient() throws NumberFormatException {
        String host = hostField.getText().trim();
        int port = Integer.parseInt(portField.getText().trim());
        boolean needNew = (sharedClient == null) || (lastHost == null) || (!host.equals(lastHost)) || (port != lastPort);
        if (needNew) {
            sharedClient = new HttpClient(host, port);
            lastHost = host;
            lastPort = port;
        }
        // 根据勾选状态设置是否启用长连接（会在关闭时清理旧的持久连接）
        sharedClient.setEnableKeepAlive(keepAliveBox.isSelected());
        return sharedClient;
    }

    private void appendResponseText(String text) {
        responseArea.append(text + "\n");
        responseArea.setCaretPosition(responseArea.getDocument().getLength());
    }

    private void setStatus(String text) {
        statusLabel.setText(" " + text);
    }

    private void clearResponse() {
        responseArea.setText("");
    }

    private void onTestConnection(ActionEvent e) {
        clearResponse();
        setStatus("测试连接中...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    HttpClient client = createClient();
                    HttpResponse response = client.get("/");
                    response = client.handleRedirect(response, 5);
                    displayResponseAndMaybeSave(response, "/");
                    setStatus("测试连接成功");
                } catch (Exception ex) {
                    appendResponseText("连接失败: " + ex.getMessage());
                    setStatus("连接失败");
                }
                return null;
            }
        }.execute();
    }

    private void onGet(ActionEvent e) {
        clearResponse();
        String uri = uriField.getText().trim();
        if (uri.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先填写 URI", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        setStatus("发送 GET 请求...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    HttpClient client = createClient();
                    HttpResponse response = client.get(uri);
                    response = client.handleRedirect(response, 5);
                    displayResponseAndMaybeSave(response, uri);
                    setStatus("GET 请求完成");
                } catch (Exception ex) {
                    appendResponseText("请求失败: " + ex.getMessage());
                    setStatus("GET 请求失败");
                }
                return null;
            }
        }.execute();
    }

    private void onPostText(ActionEvent e) {
        clearResponse();
        String uri = uriField.getText().trim();
        String body = requestBodyArea.getText();

        if (uri.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先填写 URI", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (body == null) {
            body = "";
        }
        String finalBody = body;
        setStatus("发送 POST 文本请求...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    HttpClient client = createClient();
                    byte[] bytes = finalBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    // 文本使用 text/plain，避免误设为 application/json
                    HttpResponse response = client.postBinary(uri, bytes, "text/plain");
                    displayResponseAndMaybeSave(response, uri);
                    setStatus("POST 文本请求完成");
                } catch (Exception ex) {
                    appendResponseText("请求失败: " + ex.getMessage());
                    setStatus("POST 文本请求失败");
                }
                return null;
            }
        }.execute();
    }

    private void onChooseFile(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        if (!filePathField.getText().trim().isEmpty()) {
            chooser.setSelectedFile(new File(filePathField.getText().trim()));
        }
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            filePathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void onUploadFile(ActionEvent e) {
        clearResponse();
        String uri = uriField.getText().trim();
        String path = filePathField.getText().trim();

        if (uri.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先填写 URI（例如 /api/upload）", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择要上传的文件", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            JOptionPane.showMessageDialog(this, "文件不存在: " + path, "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setStatus("上传文件中...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try (FileInputStream fis = new FileInputStream(file);
                     ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

                    byte[] tmp = new byte[4096];
                    int len;
                    while ((len = fis.read(tmp)) != -1) {
                        buffer.write(tmp, 0, len);
                    }
                    byte[] bodyBytes = buffer.toByteArray();

                    HttpClient client = createClient();
                    // 根据扩展名推断 Content-Type
                    String fname = file.getName().toLowerCase();
                    String ct;
                    if (fname.endsWith(".png")) ct = "image/png";
                    else if (fname.endsWith(".jpg") || fname.endsWith(".jpeg")) ct = "image/jpeg";
                    else if (fname.endsWith(".html") || fname.endsWith(".htm")) ct = "text/html";
                    else if (fname.endsWith(".txt")) ct = "text/plain";
                    else if (fname.endsWith(".json")) ct = "application/json";
                    else ct = "application/octet-stream";

                    HttpResponse response = client.postBinary(uri, bodyBytes, ct);
                    displayResponseAndMaybeSave(response, uri);
                    setStatus("文件上传完成");
                } catch (Exception ex) {
                    appendResponseText("上传失败: " + ex.getMessage());
                    setStatus("文件上传失败");
                }
                return null;
            }
        }.execute();
    }

    private void onRegister(ActionEvent e) {
        clearResponse();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名和密码均不能为空", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setStatus("注册中...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    HttpClient client = createClient();
                    String body = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";
                    HttpResponse response = client.post("/api/register", body.getBytes());
                    displayResponseAndMaybeSave(response, "/api/register");
                    setStatus("注册完成");
                } catch (Exception ex) {
                    appendResponseText("注册失败: " + ex.getMessage());
                    setStatus("注册失败");
                }
                return null;
            }
        }.execute();
    }

    private void onLogin(ActionEvent e) {
        clearResponse();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "用户名和密码均不能为空", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setStatus("登录中...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    HttpClient client = createClient();
                    String body = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";
                    HttpResponse response = client.post("/api/login", body.getBytes());
                    displayResponseAndMaybeSave(response, "/api/login");
                    setStatus("登录完成");
                } catch (Exception ex) {
                    appendResponseText("登录失败: " + ex.getMessage());
                    setStatus("登录失败");
                }
                return null;
            }
        }.execute();
    }

    // ========================= 响应展示与文件保存 =========================

    private void displayResponseAndMaybeSave(HttpResponse response, String uri) {
        if (response == null) {
            appendResponseText("无响应");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== HTTP 响应 ===\n");
        sb.append("状态: ").append(response.getStatusCode()).append(" ").append(response.getReasonPhrase()).append("\n\n");

        sb.append("响应头:\n");
        for (String headerName : response.getHeaders().keySet()) {
            sb.append(headerName).append(": ").append(response.getHeader(headerName)).append("\n");
        }
        sb.append("\n响应体:\n");

        byte[] body = response.getBody();
        String contentType = response.getHeader("Content-Type");

        if (body != null && body.length > 0) {
            if (contentType != null && MimeType.isTextType(contentType)) {
                sb.append(new String(body));
            } else {
                sb.append("[二进制内容，长度: ").append(body.length).append(" 字节]\n");
                // 保存二进制内容到文件
                String filename = generateFileName(uri, contentType);
                File saved = saveBinaryFile(body, filename);
                if (saved != null) {
                    sb.append("已保存到文件: ").append(saved.getAbsolutePath()).append("\n");
                } else {
                    sb.append("保存文件失败\n");
                }
            }
        } else {
            sb.append("[无响应体]");
        }

        appendResponseText(sb.toString());
    }

    // 与命令行客户端保持一致的保存逻辑
    private File saveBinaryFile(byte[] data, String fileName) {
        File dir = new File("downloads/");
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }

        File file = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            return file;
        } catch (IOException e) {
            return null;
        }
    }

    private String generateFileName(String uri, String contentType) {
        String name = uri;
        int qIndex = name.indexOf('?');
        if (qIndex >= 0) {
            name = name.substring(0, qIndex);
        }
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < name.length() - 1) {
            name = name.substring(lastSlash + 1);
        }
        if (name.isEmpty()) {
            String ext = "bin";
            if (contentType != null && contentType.contains("/")) {
                String t = contentType.split("/")[1];
                if (!t.isEmpty()) {
                    ext = t;
                }
            }
            name = "downloaded_" + System.currentTimeMillis() + "." + ext;
        }
        return name;
    }

    // ========================= main =========================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HttpClientGUI gui = new HttpClientGUI();
            gui.setVisible(true);
        });
    }
}


