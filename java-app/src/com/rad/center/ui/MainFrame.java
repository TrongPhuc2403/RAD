package com.rad.center.ui;

import com.rad.center.dao.CenterRepository;
import com.rad.center.model.User;
import com.rad.center.service.CenterService;
import com.rad.center.util.DatabaseConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame {
    private final CenterService service;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentArea = new JPanel(cardLayout);
    private final Map<String, JButton> navButtons = new LinkedHashMap<String, JButton>();

    private final DashboardPanel dashboardPanel;
    private final StudentPanel studentPanel;
    private final CoursePanel coursePanel;
    private final ClassPanel classPanel;
    private final EnrollmentPanel enrollmentPanel;
    private final TuitionPaymentPanel tuitionPaymentPanel;
    private final AttendancePanel attendancePanel;
    private final ReportPanel reportPanel;

    private final JLabel lblHeaderTitle = new JLabel("DASHBOARD TỔNG QUAN HỆ THỐNG");
    private final JLabel lblUserBadge = new JLabel();
    private final JLabel lblDbBadge = new JLabel();

    public MainFrame(final CenterService service) {
        this.service = service;
        setTitle("Hệ Thống Quản Lý Trung Tâm Ngoại Ngữ (RAD - 502050 | Lê Khải Hoàn – Trần Hữu Long – Nguyễn Trọng Phúc)");
        setSize(1360, 820);
        setMinimumSize(new Dimension(1180, 720));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        Runnable refreshAllCallback = new Runnable() {
            @Override
            public void run() {
                refreshAllPanels();
            }
        };

        dashboardPanel = new DashboardPanel(service);
        studentPanel = new StudentPanel(service, refreshAllCallback);
        coursePanel = new CoursePanel(service, refreshAllCallback);
        classPanel = new ClassPanel(service, refreshAllCallback);
        enrollmentPanel = new EnrollmentPanel(service, refreshAllCallback);
        tuitionPaymentPanel = new TuitionPaymentPanel(service, refreshAllCallback);
        attendancePanel = new AttendancePanel(service, refreshAllCallback);
        reportPanel = new ReportPanel(service);

        contentArea.add(dashboardPanel, "DASHBOARD");
        contentArea.add(studentPanel, "STUDENTS");
        contentArea.add(coursePanel, "COURSES");
        contentArea.add(classPanel, "CLASSES");
        contentArea.add(enrollmentPanel, "ENROLLMENTS");
        contentArea.add(tuitionPaymentPanel, "TUITION");
        contentArea.add(attendancePanel, "ATTENDANCE");
        contentArea.add(reportPanel, "REPORTS");

        // 1. Left Sidebar
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(265, 800));
        sidebar.setBackground(UITheme.SIDEBAR_BG);

        JPanel brandBox = new JPanel();
        brandBox.setLayout(new BoxLayout(brandBox, BoxLayout.Y_AXIS));
        brandBox.setBackground(new Color(30, 41, 59));
        brandBox.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel logoSmall = LoginFrame.createLogoLabel(90, 54);
        if (logoSmall != null) {
            logoSmall.setAlignmentX(Component.LEFT_ALIGNMENT);
            brandBox.add(logoSmall);
            brandBox.add(Box.createVerticalStrut(8));
        }

        JLabel lblBrand1 = new JLabel("TDTU LANGUAGE CENTER");
        lblBrand1.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblBrand1.setForeground(Color.WHITE);
        lblBrand1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblBrand2 = new JLabel("Dự án học tập: Mã môn 502050");
        lblBrand2.setFont(UITheme.FONT_SMALL);
        lblBrand2.setForeground(new Color(148, 163, 184));
        lblBrand2.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblBrand3 = new JLabel("Hoàn – Long – Phúc");
        lblBrand3.setFont(UITheme.FONT_BOLD);
        lblBrand3.setForeground(new Color(56, 189, 248));
        lblBrand3.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandBox.add(lblBrand1);
        brandBox.add(Box.createVerticalStrut(2));
        brandBox.add(lblBrand2);
        brandBox.add(Box.createVerticalStrut(2));
        brandBox.add(lblBrand3);
        sidebar.add(brandBox, BorderLayout.NORTH);

        JPanel menuList = new JPanel(new GridLayout(10, 1, 0, 4));
        menuList.setBackground(UITheme.SIDEBAR_BG);
        menuList.setBorder(new EmptyBorder(12, 10, 12, 10));

        addNavButton(menuList, "DASHBOARD", "1. Dashboard Tổng quan", "DASHBOARD TỔNG QUAN HỆ THỐNG");
        addNavButton(menuList, "STUDENTS", "2. Quản lý Học viên (CN1)", "CHỨC NĂNG 1: QUẢN LÝ HỒ SƠ HỌC VIÊN (FR-02)");
        addNavButton(menuList, "COURSES", "3. Quản lý Khóa học (FR-03)", "QUẢN LÝ CHƯƠNG TRÌNH & KHÓA HỌC ĐÀO TẠO (FR-03)");
        addNavButton(menuList, "CLASSES", "4. Quản lý Lớp học (CN2)", "CHỨC NĂNG 2: QUẢN LÝ LỚP HỌC & LỊCH HỌC (FR-04, FR-05)");
        addNavButton(menuList, "ENROLLMENTS", "5. Đăng ký Lớp học (CN3)", "CHỨC NĂNG 3: ĐĂNG KÝ LỚP HỌC & TẠO HỌC PHÍ (FR-06, FR-07, FR-09)");
        addNavButton(menuList, "TUITION", "6. Thu Học phí & Công nợ (CN4)", "CHỨC NĂNG 4: QUẢN LÝ HỌC PHÍ & THANH TOÁN (FR-10, FR-11)");
        addNavButton(menuList, "ATTENDANCE", "7. Điểm danh Học viên (CN5)", "CHỨC NĂNG 5: QUẢN LÝ BUỔI HỌC & ĐIỂM DANH (FR-12, FR-13, FR-14)");
        addNavButton(menuList, "REPORTS", "8. Báo cáo & Thống kê (FR-15)", "BÁO CÁO & THỐNG KÊ HOẠT ĐỘNG TRUNG TÂM (FR-15, UC-07)");

        sidebar.add(menuList, BorderLayout.CENTER);

        JPanel bottomSidebar = new JPanel(new GridLayout(2, 1, 6, 6));
        bottomSidebar.setBackground(UITheme.SIDEBAR_BG);
        bottomSidebar.setBorder(new EmptyBorder(12, 14, 16, 14));

        JButton btnResetDemo = UITheme.createButton("Khôi phục Dữ liệu Mẫu Demo", new Color(51, 65, 85), Color.WHITE);
        JButton btnLogout = UITheme.createButton("Đăng xuất / Đổi Vai trò", UITheme.DANGER, Color.WHITE);
        bottomSidebar.add(btnResetDemo);
        bottomSidebar.add(btnLogout);
        sidebar.add(bottomSidebar, BorderLayout.SOUTH);

        add(sidebar, BorderLayout.WEST);

        // 2. Right Main Content Area (Top Header + CardLayout Center)
        JPanel rightContainer = new JPanel(new BorderLayout());
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR),
                new EmptyBorder(12, 20, 12, 20)
        ));

        lblHeaderTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblHeaderTitle.setForeground(UITheme.TEXT_DARK);
        topBar.add(lblHeaderTitle, BorderLayout.WEST);

        JPanel statusRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        statusRight.setOpaque(false);

        lblDbBadge.setFont(UITheme.FONT_SMALL);
        lblDbBadge.setForeground(new Color(13, 148, 136));
        lblDbBadge.setText(DatabaseConfig.isMysqlAvailable() ? "CSDL: MySQL JDBC (Connected)" : "CSDL: Persistent DB (Synced)");

        User u = service.getCurrentUser();
        lblUserBadge.setFont(UITheme.FONT_BOLD);
        lblUserBadge.setForeground(UITheme.PRIMARY);
        lblUserBadge.setText(u != null ? (u.getFullName() + " [" + u.getRoleDisplay() + "]") : "");

        statusRight.add(lblDbBadge);
        statusRight.add(new JLabel("|"));
        statusRight.add(lblUserBadge);
        topBar.add(statusRight, BorderLayout.EAST);

        rightContainer.add(topBar, BorderLayout.NORTH);
        rightContainer.add(contentArea, BorderLayout.CENTER);
        add(rightContainer, BorderLayout.CENTER);

        btnResetDemo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(MainFrame.this,
                        "Khôi phục toàn bộ dữ liệu về trạng thái chuẩn của Kịch bản Demo (Chương 9)?",
                        "Xác nhận Reset Dữ liệu Mẫu", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    CenterRepository.getInstance().resetToSampleData();
                    refreshAllPanels();
                    JOptionPane.showMessageDialog(MainFrame.this, "Đã khôi phục dữ liệu mẫu thành công!");
                }
            }
        });

        btnLogout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                new LoginFrame(service).setVisible(true);
            }
        });

        applyRolePermissions();
        showTab("DASHBOARD");
    }

    private void addNavButton(JPanel parent, final String key, String text, final String headerTitle) {
        final JButton btn = new JButton(text);
        btn.setFont(UITheme.FONT_BOLD);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setForeground(Color.WHITE);
        btn.setBackground(UITheme.SIDEBAR_BG);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 14, 10, 14));

        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showTab(key, headerTitle);
            }
        });
        navButtons.put(key, btn);
        parent.add(btn);
    }

    public void showTab(String key) {
        Map<String, String> defaultTitles = new LinkedHashMap<String, String>();
        defaultTitles.put("DASHBOARD", "DASHBOARD TỔNG QUAN HỆ THỐNG");
        defaultTitles.put("STUDENTS", "CHỨC NĂNG 1: QUẢN LÝ HỒ SƠ HỌC VIÊN (FR-02)");
        defaultTitles.put("COURSES", "QUẢN LÝ CHƯƠNG TRÌNH & KHÓA HỌC ĐÀO TẠO (FR-03)");
        defaultTitles.put("CLASSES", "CHỨC NĂNG 2: QUẢN LÝ LỚP HỌC & LỊCH HỌC (FR-04, FR-05)");
        defaultTitles.put("ENROLLMENTS", "CHỨC NĂNG 3: ĐĂNG KÝ LỚP HỌC & TẠO HỌC PHÍ (FR-06, FR-07, FR-09)");
        defaultTitles.put("TUITION", "CHỨC NĂNG 4: QUẢN LÝ HỌC PHÍ & THANH TOÁN (FR-10, FR-11)");
        defaultTitles.put("ATTENDANCE", "CHỨC NĂNG 5: QUẢN LÝ BUỔI HỌC & ĐIỂM DANH (FR-12, FR-13, FR-14)");
        defaultTitles.put("REPORTS", "BÁO CÁO & THỐNG KÊ HOẠT ĐỘNG TRUNG TÂM (FR-15, UC-07)");
        showTab(key, defaultTitles.get(key));
    }

    public void showTab(String key, String headerTitle) {
        cardLayout.show(contentArea, key);
        if (headerTitle != null) {
            lblHeaderTitle.setText(headerTitle);
        }
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            if (entry.getKey().equals(key)) {
                entry.getValue().setBackground(UITheme.SIDEBAR_ACTIVE);
            } else {
                entry.getValue().setBackground(UITheme.SIDEBAR_BG);
            }
        }
    }

    /**
     * NFR-04 & Chương 3.3: Ma trận phân quyền theo vai trò
     */
    private void applyRolePermissions() {
        User u = service.getCurrentUser();
        if (u == null) return;
        String role = u.getRole();
        if ("CASHIER".equals(role)) {
            navButtons.get("COURSES").setEnabled(false);
            navButtons.get("ATTENDANCE").setEnabled(false);
        } else if ("TEACHER".equals(role)) {
            navButtons.get("COURSES").setEnabled(false);
            navButtons.get("ENROLLMENTS").setEnabled(false);
            navButtons.get("TUITION").setEnabled(false);
        }
    }

    public void refreshAllPanels() {
        dashboardPanel.refreshData();
        studentPanel.refreshData();
        coursePanel.refreshData();
        classPanel.refreshData();
        enrollmentPanel.refreshData();
        tuitionPaymentPanel.refreshData();
        attendancePanel.refreshData();
        reportPanel.refreshData();
    }
}
