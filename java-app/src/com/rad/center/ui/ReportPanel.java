package com.rad.center.ui;

import com.rad.center.dao.CenterRepository;
import com.rad.center.model.*;
import com.rad.center.service.CenterService;
import com.rad.center.util.FormatUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Màn hình 10 – Báo cáo & Thống kê Hoạt động Trung tâm (FR-15, UC-07)
 */
public class ReportPanel extends JPanel {
    private final CenterService service;
    private final CenterRepository repo = CenterRepository.getInstance();

    private final JLabel lblKpiDue = new JLabel("0 VNĐ");
    private final JLabel lblKpiPaid = new JLabel("0 VNĐ");
    private final JLabel lblKpiDebt = new JLabel("0 VNĐ");
    private final JLabel lblKpiRate = new JLabel("0%");

    private final DefaultTableModel classReportModel;
    private final DefaultTableModel studentAttendanceModel;

    public ReportPanel(CenterService service) {
        this.service = service;
        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_LIGHT);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 16, 0));
        kpiRow.setOpaque(false);
        kpiRow.add(UITheme.createKpiCard("TỔNG HỌC PHÍ PHẢI THU", lblKpiDue, "Theo các đăng ký hợp lệ (FR-09)", UITheme.PRIMARY));
        kpiRow.add(UITheme.createKpiCard("TỔNG DOANH THU ĐÃ THU", lblKpiPaid, "Tổng phiếu thu hợp lệ (FR-10)", UITheme.SUCCESS));
        kpiRow.add(UITheme.createKpiCard("TỔNG CÔNG NỢ HỌC PHÍ", lblKpiDebt, "Cần thu hồi (FR-11)", UITheme.DANGER));
        kpiRow.add(UITheme.createKpiCard("TỶ LỆ CHUYÊN CẦN TB", lblKpiRate, "Tính trên các buổi đã điểm danh (FR-14)", new Color(124, 58, 237)));
        add(kpiRow, BorderLayout.NORTH);

        JPanel split = new JPanel(new GridLayout(2, 1, 14, 14));
        split.setOpaque(false);

        JPanel topCard = UITheme.createCardPanel("Báo cáo 1: Thống kê Sĩ số, Học phí Phải thu, Đã thu & Công nợ theo từng Lớp học (FR-15)");
        classReportModel = new DefaultTableModel(new Object[]{
                "Mã Lớp", "Khóa học", "Giảng viên", "Sĩ số ĐK / Tối đa", "Tỷ lệ lấp đầy", "Tổng Phải thu", "Tổng Đã đóng", "Tổng Còn nợ", "Trạng thái Lớp"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable classTable = new JTable(classReportModel);
        UITheme.styleTable(classTable);
        topCard.add(new JScrollPane(classTable), BorderLayout.CENTER);

        JPanel bottomCard = UITheme.createCardPanel("Báo cáo 2: Thống kê Chuyên cần & Tình trạng Học phí theo Học viên (FR-14, FR-15)");
        studentAttendanceModel = new DefaultTableModel(new Object[]{
                "Mã HV", "Họ và tên Học viên", "Lớp tham gia", "Số buổi Có mặt", "Số buổi Vắng có phép", "Số buổi Vắng không phép", "Tỷ lệ Chuyên cần", "Tình trạng Học phí"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable attTable = new JTable(studentAttendanceModel);
        UITheme.styleTable(attTable);
        bottomCard.add(new JScrollPane(attTable), BorderLayout.CENTER);

        split.add(topCard);
        split.add(bottomCard);
        add(split, BorderLayout.CENTER);

        refreshData();
    }

    public void refreshData() {
        lblKpiDue.setText(FormatUtil.formatVnd(service.getTotalTuitionDueAll()));
        lblKpiPaid.setText(FormatUtil.formatVnd(service.getTotalCollectedAll()));
        lblKpiDebt.setText(FormatUtil.formatVnd(service.getTotalDebtAll()));

        int totalAtt = repo.getAttendances().size();
        int totalPresent = 0;
        for (Attendance a : repo.getAttendances()) {
            if ("PRESENT".equals(a.getStatus())) totalPresent++;
        }
        double avgRate = totalAtt > 0 ? (totalPresent * 100.0 / totalAtt) : 0.0;
        lblKpiRate.setText(String.format("%.1f%% (%d/%d bản ghi)", avgRate, totalPresent, totalAtt));

        classReportModel.setRowCount(0);
        for (ClassRoom cl : repo.getClasses()) {
            Course course = repo.findCourseById(cl.getCourseId());
            Teacher teacher = repo.findTeacherById(cl.getTeacherId());
            int enrolled = service.getConfirmedEnrollmentCount(cl.getClassId());
            double fillRate = cl.getCapacity() > 0 ? (enrolled * 100.0 / cl.getCapacity()) : 0.0;

            long classDue = 0L, classPaid = 0L, classDebt = 0L;
            for (Enrollment e : repo.getEnrollments()) {
                if (e.getClassId() == cl.getClassId() && !"CANCELLED".equals(e.getStatus())) {
                    TuitionFee tf = repo.findTuitionByEnrollmentId(e.getEnrollmentId());
                    if (tf != null) {
                        classDue += tf.getAmountDue();
                        classPaid += service.getTotalPaidForTuition(tf.getTuitionId());
                        classDebt += service.getRemainingDebt(tf.getTuitionId());
                    }
                }
            }
            classReportModel.addRow(new Object[]{
                    cl.getClassCode(),
                    course != null ? course.getCourseName() : "",
                    teacher != null ? teacher.getFullName() : "",
                    enrolled + " / " + cl.getCapacity(),
                    String.format("%.0f%%", fillRate),
                    FormatUtil.formatVnd(classDue),
                    FormatUtil.formatVnd(classPaid),
                    FormatUtil.formatVnd(classDebt),
                    cl.getStatus()
            });
        }

        studentAttendanceModel.setRowCount(0);
        for (Enrollment e : repo.getEnrollments()) {
            if (!"CONFIRMED".equals(e.getStatus())) continue;
            Student s = repo.findStudentById(e.getStudentId());
            ClassRoom cl = repo.findClassById(e.getClassId());
            if (s == null || cl == null) continue;

            List<Session> sessions = service.getSessionsByClass(cl.getClassId());
            int pCount = 0, exCount = 0, unexCount = 0;
            for (Session ses : sessions) {
                Attendance a = service.getAttendanceRecord(ses.getSessionId(), s.getStudentId());
                if (a != null) {
                    if ("PRESENT".equals(a.getStatus())) pCount++;
                    else if ("ABSENT_EXCUSED".equals(a.getStatus())) exCount++;
                    else unexCount++;
                }
            }
            double rate = service.calculateStudentAttendanceRate(cl.getClassId(), s.getStudentId());
            TuitionFee tf = repo.findTuitionByEnrollmentId(e.getEnrollmentId());
            String feeStatus = tf != null ? (tf.getStatus() + " (Nợ: " + FormatUtil.formatVnd(service.getRemainingDebt(tf.getTuitionId())) + ")") : "";

            studentAttendanceModel.addRow(new Object[]{
                    s.getStudentCode(),
                    s.getFullName(),
                    cl.getClassCode(),
                    pCount + " buổi",
                    exCount + " buổi",
                    unexCount + " buổi",
                    String.format("%.0f%%", rate),
                    feeStatus
            });
        }
    }
}

