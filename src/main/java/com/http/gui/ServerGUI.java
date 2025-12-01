package com.http.gui;

import com.http.server.HttpServer;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;

public class ServerGUI extends JFrame {
    private JTextField portField;
    private JTextArea logArea;
    private JButton startButton;
    private JButton stopButton;
    private JButton browserButton;
    private HttpServer server;
    private Thread serverThread;

    public ServerGUI() {
        setTitle("HTTP Server Control Panel");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        redirectSystemStreams();
    }

    private void initComponents() {
        // Top Panel for controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        controlPanel.add(new JLabel("Port:"));
        portField = new JTextField("6175", 5);
        controlPanel.add(portField);

        startButton = new JButton("Start Server");
        startButton.addActionListener(e -> startServer());
        controlPanel.add(startButton);

        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopServer());
        controlPanel.add(stopButton);

        browserButton = new JButton("Open Browser");
        browserButton.addActionListener(e -> openBrowser());
        controlPanel.add(browserButton);

        add(controlPanel, BorderLayout.NORTH);

        // Center Panel for logs
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        // Auto-scroll
        DefaultCaret caret = (DefaultCaret) logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Server Logs"));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearButton = new JButton("Clear Logs");
        clearButton.addActionListener(e -> logArea.setText(""));
        bottomPanel.add(clearButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            server = new HttpServer(port);
            
            serverThread = new Thread(() -> {
                server.start();
            });
            serverThread.start();

            startButton.setEnabled(false);
            portField.setEnabled(false);
            stopButton.setEnabled(true);
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
        }
        startButton.setEnabled(true);
        portField.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private void openBrowser() {
        try {
            String port = portField.getText().trim();
            Desktop.getDesktop().browse(new URI("http://localhost:" + port));
        } catch (Exception e) {
            System.err.println("Failed to open browser: " + e.getMessage());
        }
    }

    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                updateLog(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                updateLog(new String(b, off, len));
            }

            @Override
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }
        };

        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    private void updateLog(String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(text);
        });
    }

    public static void main(String[] args) {
        try {
            // Set System Look and Feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new ServerGUI().setVisible(true);
        });
    }
}
