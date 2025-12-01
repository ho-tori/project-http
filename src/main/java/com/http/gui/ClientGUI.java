package com.http.gui;

import com.http.client.HttpClient;
import com.http.common.HttpResponse;
import com.http.common.HttpRequest;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ClientGUI extends JFrame {
    private JComboBox<String> methodComboBox;
    private JTextField urlField;
    private JButton sendButton;
    
    // Request components
    private DefaultTableModel requestHeadersModel;
    private JTextArea requestBodyArea;
    private JButton loadFileButton;

    // Response components
    private JLabel statusLabel;
    private DefaultTableModel responseHeadersModel;
    private JTextArea responseBodyArea;
    private JTabbedPane mainTabbedPane;

    public ClientGUI() {
        setTitle("HTTP Client");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        // Top Panel: Method, URL, Send
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] methods = {"GET", "POST", "PUT", "DELETE", "HEAD"};
        methodComboBox = new JComboBox<>(methods);
        topPanel.add(methodComboBox, BorderLayout.WEST);

        urlField = new JTextField("http://localhost:6175/");
        topPanel.add(urlField, BorderLayout.CENTER);

        sendButton = new JButton("Send Request");
        sendButton.addActionListener(e -> sendRequest());
        topPanel.add(sendButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Main Content: Request and Response Split
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);

        // Request Panel
        JTabbedPane requestTabs = new JTabbedPane();
        requestTabs.addTab("Request Body", createRequestBodyPanel());
        requestTabs.addTab("Request Headers", createRequestHeadersPanel());
        splitPane.setTopComponent(requestTabs);

        // Response Panel
        JPanel responsePanel = new JPanel(new BorderLayout());
        responsePanel.setBorder(BorderFactory.createTitledBorder("Response"));
        
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        responsePanel.add(statusLabel, BorderLayout.NORTH);

        JTabbedPane responseTabs = new JTabbedPane();
        responseTabs.addTab("Response Body", createResponseBodyPanel());
        responseTabs.addTab("Response Headers", createResponseHeadersPanel());
        responsePanel.add(responseTabs, BorderLayout.CENTER);

        splitPane.setBottomComponent(responsePanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createRequestBodyPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        requestBodyArea = new JTextArea();
        requestBodyArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(new JScrollPane(requestBodyArea), BorderLayout.CENTER);

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        loadFileButton = new JButton("Load File Content...");
        loadFileButton.addActionListener(e -> loadFileToBody());
        toolBar.add(loadFileButton);
        
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> requestBodyArea.setText(""));
        toolBar.add(clearButton);

        panel.add(toolBar, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createRequestHeadersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columns = {"Key", "Value"};
        requestHeadersModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(requestHeadersModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add Header");
        addButton.addActionListener(e -> requestHeadersModel.addRow(new Object[]{"", ""}));
        toolBar.add(addButton);

        JButton removeButton = new JButton("Remove Selected");
        removeButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) requestHeadersModel.removeRow(row);
        });
        toolBar.add(removeButton);

        // Add default headers
        requestHeadersModel.addRow(new Object[]{"User-Agent", "Java-HTTP-Client-GUI"});
        requestHeadersModel.addRow(new Object[]{"Accept", "*/*"});

        panel.add(toolBar, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createResponseBodyPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        responseBodyArea = new JTextArea();
        responseBodyArea.setEditable(false);
        responseBodyArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(new JScrollPane(responseBodyArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createResponseHeadersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columns = {"Key", "Value"};
        responseHeadersModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(responseHeadersModel);
        table.setEnabled(false);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void loadFileToBody() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] data = new byte[(int) file.length()];
                fis.read(data);
                requestBodyArea.setText(new String(data, StandardCharsets.UTF_8));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error reading file: " + ex.getMessage());
            }
        }
    }

    private void sendRequest() {
        new Thread(() -> {
            try {
                sendButton.setEnabled(false);
                statusLabel.setText("Sending request...");
                
                String urlStr = urlField.getText().trim();
                if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
                    urlStr = "http://" + urlStr;
                }
                
                URI uri = new URI(urlStr);
                String host = uri.getHost();
                int port = uri.getPort();
                if (port == -1) port = 80;
                
                String path = uri.getRawPath();
                if (path == null || path.isEmpty()) path = "/";
                if (uri.getRawQuery() != null) path += "?" + uri.getRawQuery();

                HttpClient client = new HttpClient(host, port);
                
                String method = (String) methodComboBox.getSelectedItem();
                HttpRequest request = new HttpRequest(method, path);
                
                // Add Headers
                for (int i = 0; i < requestHeadersModel.getRowCount(); i++) {
                    String key = (String) requestHeadersModel.getValueAt(i, 0);
                    String value = (String) requestHeadersModel.getValueAt(i, 1);
                    if (key != null && !key.trim().isEmpty()) {
                        request.addHeader(key.trim(), value != null ? value.trim() : "");
                    }
                }
                
                // Add Host header if not present
                if (request.getHeaders().get("Host") == null) {
                    request.addHeader("Host", host + ":" + port);
                }
                
                // Add Body
                String bodyText = requestBodyArea.getText();
                if (!bodyText.isEmpty()) {
                    byte[] bodyBytes = bodyText.getBytes(StandardCharsets.UTF_8);
                    request.setBody(bodyBytes);
                    if (request.getHeaders().get("Content-Length") == null) {
                        request.addHeader("Content-Length", String.valueOf(bodyBytes.length));
                    }
                    if (request.getHeaders().get("Content-Type") == null) {
                        // Default to text/plain or application/x-www-form-urlencoded depending on content?
                        // Let's default to text/plain for simplicity unless user set it
                        request.addHeader("Content-Type", "text/plain");
                    }
                }

                HttpResponse response = client.sendRequest(request);
                
                // Handle Redirects (Simple implementation reusing client logic if needed, 
                // but here we just show the first response or manually handle it? 
                // HttpClient.handleRedirect logic is coupled with ConsoleWriter.
                // Let's just show the response we got. If it's 3xx, user can see Location header.)

                SwingUtilities.invokeLater(() -> displayResponse(response));

            } catch (URISyntaxException e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Error: Invalid URL");
                    JOptionPane.showMessageDialog(this, "Invalid URL format");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    responseBodyArea.setText("Connection failed: " + e.toString());
                });
            } finally {
                SwingUtilities.invokeLater(() -> sendButton.setEnabled(true));
            }
        }).start();
    }

    private void displayResponse(HttpResponse response) {
        statusLabel.setText(response.getVersion() + " " + response.getStatusCode() + " " + response.getReasonPhrase());
        
        // Clear and fill headers
        responseHeadersModel.setRowCount(0);
        for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
            responseHeadersModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }

        // Fill body
        if (response.getBody() != null) {
            String contentType = response.getHeader("Content-Type");
            // Simple check if it's text
            if (contentType != null && (contentType.contains("text") || contentType.contains("json") || contentType.contains("xml") || contentType.contains("html"))) {
                responseBodyArea.setText(new String(response.getBody(), StandardCharsets.UTF_8));
                responseBodyArea.setCaretPosition(0);
            } else {
                responseBodyArea.setText("[Binary content: " + response.getBody().length + " bytes]");
            }
        } else {
            responseBodyArea.setText("");
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new ClientGUI().setVisible(true));
    }
}
