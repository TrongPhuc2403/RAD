package com.rad.center.ui;

import com.rad.center.server.WebAppServer;
import com.rad.center.service.CenterService;

import javax.swing.*;

/**
 * Điểm khởi chạy chính của chương trình Java (Mục 3.2 Đề thi cuối kỳ).
 * Hỗ trợ:
 * - Chạy đồng thời giao diện Desktop Java Swing + HTTP REST/Web Server tại http://localhost:8080
 * - Tham số --web-only để chỉ khởi chạy HTTP Server
 * - Tham số --direct để vào thẳng MainFrame
 */
public class LanguageCenterApp {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        final CenterService service = new CenterService();

        // Khởi chạy tích hợp HTTP REST API & Web Server trên cổng 8080
        try {
            WebAppServer webServer = new WebAppServer(8080, service);
            webServer.start();
        } catch (Exception ex) {
            System.out.println("Thông báo cổng 8080: " + ex.getMessage());
        }

        if (args.length > 0 && "--web-only".equalsIgnoreCase(args[0])) {
            return;
        }

        if (args.length > 0 && "--direct".equalsIgnoreCase(args[0])) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    new MainFrame(service).setVisible(true);
                }
            });
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    new LoginFrame(service).setVisible(true);
                }
            });
        }
    }
}
