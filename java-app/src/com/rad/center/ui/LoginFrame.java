package com.rad.center.ui;

import com.rad.center.model.User;
import com.rad.center.service.CenterService;
import com.rad.center.service.ValidationException;
import com.rad.center.util.DatabaseConfig;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Màn hình 1 – Đăng nhập & Phân quyền Hệ thống (FR-01, UC-01, NFR-02, NFR-03, NFR-04)
 */
public class LoginFrame extends JFrame {
    private final CenterService service;
    private final JTextField txtUsername = UITheme.createTextField(20);
    private final JPasswordField txtPassword = new JPasswordField(20);

    public LoginFrame(final CenterService service) {
        this.service = service;
        setTitle("Đăng nhập Hệ thống Quản lý Trung tâm Ngoại ngữ (RAD - 502050 | Lê Khải Hoàn – Trần Hữu Long – Nguyễn Trọng Phúc)");
        setSize(960, 580);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new GridLayout(1, 2));

        // Left Banner Panel
        JPanel leftPanel = new JPanel(new BorderLayout(16, 16));
        leftPanel.setBackground(UITheme.SIDEBAR_BG);
        leftPanel.setBorder(new EmptyBorder(32, 34, 32, 34));

        JPanel brandInfo = new JPanel();
        brandInfo.setLayout(new BoxLayout(brandInfo, BoxLayout.Y_AXIS));
        brandInfo.setOpaque(false);

        JLabel logoLabel = createLogoLabel(130, 78);
        if (logoLabel != null) {
            logoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            brandInfo.add(logoLabel);
            brandInfo.add(Box.createVerticalStrut(12));
        }

        JLabel l1 = new JLabel("ĐẠI HỌC TÔN ĐỨC THẮNG (TDTU) – KHOA CNTT");
        l1.setFont(new Font("SansSerif", Font.BOLD, 13));
        l1.setForeground(new Color(56, 189, 248));
        l1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel l2 = new JLabel("HỆ THỐNG QUẢN LÝ");
        l2.setFont(new Font("SansSerif", Font.BOLD, 22));
        l2.setForeground(Color.WHITE);
        l2.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel l3 = new JLabel("TRUNG TÂM NGOẠI NGỮ");
        l3.setFont(new Font("SansSerif", Font.BOLD, 22));
        l3.setForeground(Color.WHITE);
        l3.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel l4 = new JLabel("Dự án học tập: Phân tích và Thiết kế Yêu cầu (502050)");
        l4.setFont(UITheme.FONT_REGULAR);
        l4.setForeground(new Color(203, 213, 225));
        l4.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel l5 = new JLabel("Nhóm thực hiện: Lê Khải Hoàn – Trần Hữu Long – Nguyễn Trọng Phúc");
        l5.setFont(UITheme.FONT_BOLD);
        l5.setForeground(new Color(16, 185, 129));
        l5.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandInfo.add(l1);
        brandInfo.add(Box.createVerticalStrut(6));
        brandInfo.add(l2);
        brandInfo.add(Box.createVerticalStrut(2));
        brandInfo.add(l3);
        brandInfo.add(Box.createVerticalStrut(8));
        brandInfo.add(l4);
        brandInfo.add(Box.createVerticalStrut(4));
        brandInfo.add(l5);
        leftPanel.add(brandInfo, BorderLayout.NORTH);

        JTextArea featureSummary = new JTextArea(
                "3 HỒ SƠ THÀNH VIÊN KIỂM THỬ:\n"
                        + "1. Lê Khải Hoàn (thungan / 123456) – Nhân viên Thu ngân\n"
                        + "2. Trần Hữu Long (admin / 123456) – Quản trị viên\n"
                        + "3. Nguyễn Trọng Phúc (daotao / 123456) – Nhân viên Đào tạo\n\n"
                        + "5 CHỨC NĂNG TRỌNG TÂM:\n"
                        + "• CN1: Quản lý Hồ sơ Học viên (FR-02)\n"
                        + "• CN2: Quản lý Khóa học & Lớp học (FR-03, FR-04)\n"
                        + "• CN3: Đăng ký Lớp học & Tự động tạo Học phí (FR-06, FR-09)\n"
                        + "• CN4: Thu Học phí & Quản lý Công nợ (FR-10, FR-11)\n"
                        + "• CN5: Điểm danh Học viên & Chuyên cần (FR-12..FR-14)\n\n"
                        + "Trạng thái CSDL:\n" + DatabaseConfig.getConnectionSummary()
        );
        featureSummary.setEditable(false);
        featureSummary.setOpaque(false);
        featureSummary.setForeground(new Color(226, 232, 240));
        featureSummary.setFont(UITheme.FONT_REGULAR);
        leftPanel.add(featureSummary, BorderLayout.CENTER);

        // Right Login Form Panel
        JPanel rightPanel = new JPanel(new BorderLayout(12, 12));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBorder(new EmptyBorder(32, 38, 32, 38));

        JLabel formTitle = new JLabel("ĐĂNG NHẬP HỆ THỐNG (UC-01)");
        formTitle.setFont(UITheme.FONT_TITLE);
        formTitle.setForeground(UITheme.TEXT_DARK);
        rightPanel.add(formTitle, BorderLayout.NORTH);

        JPanel formCenter = new JPanel(new GridLayout(6, 1, 6, 6));
        formCenter.setOpaque(false);

        txtUsername.setText("admin");
        txtPassword.setText("123456");
        txtPassword.setFont(UITheme.FONT_REGULAR);
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                new EmptyBorder(6, 8, 6, 8)
        ));

        JLabel lblU = new JLabel("Tên đăng nhập (*):");
        lblU.setFont(UITheme.FONT_BOLD);
        JLabel lblP = new JLabel("Mật khẩu (* - Băm SHA-256 theo NFR-03):");
        lblP.setFont(UITheme.FONT_BOLD);

        JButton btnLogin = UITheme.createButton("ĐĂNG NHẬP VÀO HỆ THỐNG", UITheme.PRIMARY, Color.WHITE);

        formCenter.add(lblU);
        formCenter.add(txtUsername);
        formCenter.add(lblP);
        formCenter.add(txtPassword);
        formCenter.add(new JLabel(""));
        formCenter.add(btnLogin);
        rightPanel.add(formCenter, BorderLayout.CENTER);

        // Quick Role Selector for Demo
        JPanel quickDemoPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        quickDemoPanel.setOpaque(false);
        quickDemoPanel.setBorder(BorderFactory.createTitledBorder("Hồ sơ Kiểm thử 3 Thành viên & Giảng viên (Mật khẩu: 123456)"));

        JButton btnRoleAdmin = UITheme.createButton("Trần Hữu Long (Admin)", new Color(241, 245, 249), UITheme.TEXT_DARK);
        JButton btnRoleTraining = UITheme.createButton("Nguyễn Trọng Phúc (Đào tạo)", new Color(241, 245, 249), UITheme.TEXT_DARK);
        JButton btnRoleCashier = UITheme.createButton("Lê Khải Hoàn (Thu ngân)", new Color(241, 245, 249), UITheme.TEXT_DARK);
        JButton btnRoleTeacher = UITheme.createButton("ThS. Hoàng Minh Tuấn (GV)", new Color(241, 245, 249), UITheme.TEXT_DARK);

        quickDemoPanel.add(btnRoleAdmin);
        quickDemoPanel.add(btnRoleTraining);
        quickDemoPanel.add(btnRoleCashier);
        quickDemoPanel.add(btnRoleTeacher);
        rightPanel.add(quickDemoPanel, BorderLayout.SOUTH);

        add(leftPanel);
        add(rightPanel);

        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doLogin();
            }
        });

        btnRoleAdmin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtUsername.setText("admin");
                txtPassword.setText("123456");
            }
        });
        btnRoleTraining.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtUsername.setText("daotao");
                txtPassword.setText("123456");
            }
        });
        btnRoleCashier.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtUsername.setText("thungan");
                txtPassword.setText("123456");
            }
        });
        btnRoleTeacher.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtUsername.setText("giangvien");
                txtPassword.setText("123456");
            }
        });
    }

    public static JLabel createLogoLabel(int targetW, int targetH) {
        String[] paths = new String[]{
                "assets/tdtu-logo.png",
                "../prototype/assets/tdtu-logo.png",
                "java-app/assets/tdtu-logo.png"
        };
        for (String p : paths) {
            File f = new File(p);
            if (f.exists()) {
                try {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) {
                        Image scaled = img.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
                        JLabel lbl = new JLabel(new ImageIcon(scaled));
                        lbl.setOpaque(true);
                        lbl.setBackground(Color.WHITE);
                        lbl.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                                new EmptyBorder(6, 10, 6, 10)
                        ));
                        return lbl;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private void doLogin() {
        try {
            User user = service.login(txtUsername.getText(), new String(txtPassword.getPassword()));
            dispose();
            MainFrame mainFrame = new MainFrame(service);
            mainFrame.setVisible(true);
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this,
                    "[" + ex.getRuleCode() + "] " + ex.getMessage(),
                    "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
        }
    }
}
