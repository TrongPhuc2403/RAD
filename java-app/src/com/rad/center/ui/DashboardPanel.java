package com.rad.center.ui;

import com.rad.center.dao.CenterRepository;
import com.rad.center.model.*;
import com.rad.center.service.CenterService;
import com.rad.center.util.FormatUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class DashboardPanel extends JPanel {
    private final CenterService service;
    private final CenterRepository repo = CenterRepository.getInstance();

    private final JLabel lblTotalStudents = new JLabel("0");
    private final JLabel lblActiveClasses = new JLabel("0");
    private final JLabel lblTotalCollected = new JLabel("0 VNĐ");
    private final JLabel lblTotalDebt = new JLabel("0 VNĐ");

    private final DefaultTableModel classModel;
    private final DefaultTableModel paymentModel;

    public DashboardPanel(CenterService service) {
        this.service = service;
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_LIGHT);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // Top KPI Row
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        kpiGrid.setOpaque(false);
        kpiGrid.add(UITheme.createKpiCard("TỔNG HỌC VIÊN HỆ THỐNG", lblTotalStudents, "Hồ sơ đang hoạt động (FR-02)", UITheme.PRIMARY));
        kpiGrid.add(UITheme.createKpiCard("LỚP HỌC ĐANG MỞ / HỌC", lblActiveClasses, "Kiểm soát sĩ số tập trung (FR-04)", UITheme.SUCCESS));
        kpiGrid.add(UITheme.createKpiCard("TỔNG HỌC PHÍ ĐÃ THU", lblTotalCollected, "Ghi nhận giao dịch hợp lệ (FR-10)", new Color(13, 148, 136)));
        kpiGrid.add(UITheme.createKpiCard("TỔNG CÔNG NỢ CÒN LẠI", lblTotalDebt, "Còn nợ = Phải thu - Đã đóng (BR-19)", UITheme.WARNING));
        add(kpiGrid, BorderLayout.NORTH);

        // Center Tables
        JPanel centerGrid = new JPanel(new GridLayout(1, 2, 16, 0));
        centerGrid.setOpaque(false);

        JPanel leftCard = UITheme.createCardPanel("Tình trạng Sĩ số các Lớp học tiêu biểu (BR-05, BR-06)");
        classModel = new DefaultTableModel(new Object[]{
                "Mã lớp", "Khóa học", "Giảng viên", "Lịch học", "Sĩ số (Đã ĐK/Tối đa)", "Trạng thái"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable classTable = new JTable(classModel);
        UITheme.styleTable(classTable);
        leftCard.add(new JScrollPane(classTable), BorderLayout.CENTER);

        JPanel rightCard = UITheme.createCardPanel("Giao dịch Thanh toán Học phí gần đây (FR-10, FR-11)");
        paymentModel = new DefaultTableModel(new Object[]{
                "Mã GD", "Học viên", "Lớp", "Số tiền đóng", "Phương thức", "Ngày thu"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable paymentTable = new JTable(paymentModel);
        UITheme.styleTable(paymentTable);
        rightCard.add(new JScrollPane(paymentTable), BorderLayout.CENTER);

        centerGrid.add(leftCard);
        centerGrid.add(rightCard);
        add(centerGrid, BorderLayout.CENTER);

        refreshData();
    }

    public void refreshData() {
        lblTotalStudents.setText(repo.getStudents().size() + " học viên");
        int openCount = 0;
        for (ClassRoom c : repo.getClasses()) {
            if ("OPEN".equals(c.getStatus()) || "ONGOING".equals(c.getStatus())) openCount++;
        }
        lblActiveClasses.setText(openCount + " / " + repo.getClasses().size() + " lớp");
        lblTotalCollected.setText(FormatUtil.formatVnd(service.getTotalCollectedAll()));
        lblTotalDebt.setText(FormatUtil.formatVnd(service.getTotalDebtAll()));

        classModel.setRowCount(0);
        for (ClassRoom c : repo.getClasses()) {
            Course course = repo.findCourseById(c.getCourseId());
            Teacher teacher = repo.findTeacherById(c.getTeacherId());
            int enrolled = service.getConfirmedEnrollmentCount(c.getClassId());
            String capStr = enrolled + " / " + c.getCapacity() + (enrolled >= c.getCapacity() ? " (ĐẦY)" : "");
            classModel.addRow(new Object[]{
                    c.getClassCode(),
                    course != null ? course.getCourseName() : "",
                    teacher != null ? teacher.getFullName() : "",
                    c.getScheduleSummary(),
                    capStr,
                    c.getStatus()
            });
        }

        paymentModel.setRowCount(0);
        for (int i = repo.getPayments().size() - 1; i >= 0; i--) {
            Payment p = repo.getPayments().get(i);
            TuitionFee tf = repo.findTuitionById(p.getTuitionId());
            Enrollment e = tf != null ? repo.findEnrollmentById(tf.getEnrollmentId()) : null;
            Student s = e != null ? repo.findStudentById(e.getStudentId()) : null;
            ClassRoom c = e != null ? repo.findClassById(e.getClassId()) : null;
            paymentModel.addRow(new Object[]{
                    p.getPaymentCode(),
                    s != null ? s.getFullName() : "",
                    c != null ? c.getClassCode() : "",
                    FormatUtil.formatVnd(p.getAmount()),
                    p.getMethod(),
                    p.getPaymentDate()
            });
        }
    }
}

