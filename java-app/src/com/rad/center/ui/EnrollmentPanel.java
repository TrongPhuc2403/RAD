package com.rad.center.ui;

import com.rad.center.dao.CenterRepository;
import com.rad.center.model.*;
import com.rad.center.service.CenterService;
import com.rad.center.service.ValidationException;
import com.rad.center.util.FormatUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Chức năng 3 – Đăng ký lớp học (FR-06, FR-07, FR-08, FR-09, UC-04, BR-08 -> BR-15, TC-03, TC-04, TC-05)
 */
public class EnrollmentPanel extends JPanel {
    private final CenterService service;
    private final CenterRepository repo = CenterRepository.getInstance();
    private final Runnable onDataChanged;

    private final JComboBox<Student> cboStudent = new JComboBox<Student>();
    private final JComboBox<ClassRoom> cboClass = new JComboBox<ClassRoom>();
    private final JLabel lblClassInfo = new JLabel("Chọn lớp để xem sĩ số hiện tại & học phí phát sinh...");

    private final DefaultTableModel tableModel;
    private final JTable table;

    public EnrollmentPanel(final CenterService service, final Runnable onDataChanged) {
        this.service = service;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_LIGHT);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel formCard = UITheme.createCardPanel("Chức năng 3: Đăng ký Lớp học & Tự động Xác định Nghĩa vụ Học phí (FR-06, FR-07, FR-09 | BR-06, BR-10, BR-11)");
        JPanel formGrid = new JPanel(new GridBagLayout());
        formGrid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 10, 6, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        JLabel l1 = new JLabel("Chọn Học viên (*):");
        l1.setFont(UITheme.FONT_BOLD);
        formGrid.add(l1, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        formGrid.add(cboStudent, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        JLabel l2 = new JLabel("Chọn Lớp học (*):");
        l2.setFont(UITheme.FONT_BOLD);
        formGrid.add(l2, gbc);

        gbc.gridx = 3; gbc.weightx = 1.0;
        formGrid.add(cboClass, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4;
        lblClassInfo.setFont(UITheme.FONT_BOLD);
        lblClassInfo.setForeground(UITheme.PRIMARY);
        formGrid.add(lblClassInfo, gbc);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        actionRow.setOpaque(false);
        JButton btnEnroll = UITheme.createButton("+ Xác nhận Đăng ký & Tạo Học phí (UC-04)", UITheme.PRIMARY, Color.WHITE);
        JButton btnCancel = UITheme.createButton("Hủy đăng ký đã chọn (FR-08)", UITheme.DANGER, Color.WHITE);
        actionRow.add(btnEnroll);
        actionRow.add(btnCancel);

        formCard.add(formGrid, BorderLayout.CENTER);
        formCard.add(actionRow, BorderLayout.SOUTH);
        add(formCard, BorderLayout.NORTH);

        JPanel tableCard = UITheme.createCardPanel("Danh sách Bản ghi Đăng ký Lớp học & Nghĩa vụ Học phí Liên kết");
        tableModel = new DefaultTableModel(new Object[]{
                "ID", "Mã ĐK", "Mã HV", "Họ tên Học viên", "Mã Lớp", "Khóa học", "Sĩ số Lớp", "Ngày ĐK", "Học phí Phát sinh", "Trạng thái ĐK", "Ghi chú / Lý do hủy"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        cboClass.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateClassPreview();
            }
        });

        btnEnroll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Student s = (Student) cboStudent.getSelectedItem();
                ClassRoom c = (ClassRoom) cboClass.getSelectedItem();
                if (s == null || c == null) return;
                try {
                    Enrollment created = service.enrollStudent(s.getStudentId(), c.getClassId());
                    TuitionFee tf = repo.findTuitionByEnrollmentId(created.getEnrollmentId());
                    refreshData();
                    if (onDataChanged != null) onDataChanged.run();
                    JOptionPane.showMessageDialog(EnrollmentPanel.this,
                            "Đăng ký thành công!\n"
                                    + "- Mã đăng ký: " + created.getEnrollmentCode() + "\n"
                                    + "- Học viên: " + s.getFullName() + "\n"
                                    + "- Lớp học: " + c.getClassCode() + "\n"
                                    + "- Học phí phải thu tự động tạo (FR-09): " + (tf != null ? FormatUtil.formatVnd(tf.getAmountDue()) : ""),
                            "Thành công (TC-03 / UC-04)", JOptionPane.INFORMATION_MESSAGE);
                } catch (ValidationException ex) {
                    JOptionPane.showMessageDialog(EnrollmentPanel.this,
                            "[" + ex.getRuleCode() + "] " + ex.getMessage(),
                            "Từ chối đăng ký (Kiểm tra điều kiện FR-07)", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(EnrollmentPanel.this, "Vui lòng chọn một bản ghi đăng ký để hủy!");
                    return;
                }
                int enrollmentId = (Integer) tableModel.getValueAt(row, 0);
                String reason = JOptionPane.showInputDialog(EnrollmentPanel.this,
                        "Nhập lý do hủy đăng ký (BR-12: Giải phóng sĩ số, BR-13: Giữ nguyên lịch sử thanh toán):",
                        "Học viên thay đổi kế hoạch học tập");
                if (reason == null) return;
                try {
                    service.cancelEnrollment(enrollmentId, reason);
                    refreshData();
                    if (onDataChanged != null) onDataChanged.run();
                    JOptionPane.showMessageDialog(EnrollmentPanel.this, "Đã chuyển trạng thái đăng ký sang CANCELLED và giải phóng sĩ số lớp!");
                } catch (ValidationException ex) {
                    JOptionPane.showMessageDialog(EnrollmentPanel.this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        refreshData();
        if (tableModel.getRowCount() > 0) table.setRowSelectionInterval(0, 0);
    }

    private void updateClassPreview() {
        ClassRoom c = (ClassRoom) cboClass.getSelectedItem();
        if (c == null) return;
        Course course = repo.findCourseById(c.getCourseId());
        Teacher teacher = repo.findTeacherById(c.getTeacherId());
        int enrolled = service.getConfirmedEnrollmentCount(c.getClassId());
        String statusText = enrolled >= c.getCapacity() ? "ĐÃ ĐẦY SĨ SỐ (Sẽ từ chối theo TC-04)" : ("Còn nhận " + (c.getCapacity() - enrolled) + " học viên");
        lblClassInfo.setText(String.format(
                "Thông tin kiểm tra tự động (FR-07): Khóa [%s] | GV: %s | Sĩ số hiện tại: %d/%d (%s) | Trạng thái lớp: %s | Học phí phát sinh (FR-09): %s",
                course != null ? course.getCourseName() : "",
                teacher != null ? teacher.getFullName() : "",
                enrolled, c.getCapacity(), statusText,
                c.getStatus(),
                course != null ? FormatUtil.formatVnd(course.getTuition()) : "0 VNĐ"
        ));
    }

    public void refreshData() {
        cboStudent.removeAllItems();
        for (Student s : repo.getStudents()) {
            cboStudent.addItem(s);
        }

        cboClass.removeAllItems();
        for (ClassRoom c : repo.getClasses()) {
            cboClass.addItem(c);
        }
        updateClassPreview();

        tableModel.setRowCount(0);
        for (Enrollment e : repo.getEnrollments()) {
            Student s = repo.findStudentById(e.getStudentId());
            ClassRoom c = repo.findClassById(e.getClassId());
            Course course = c != null ? repo.findCourseById(c.getCourseId()) : null;
            TuitionFee tf = repo.findTuitionByEnrollmentId(e.getEnrollmentId());
            int enrolledCount = c != null ? service.getConfirmedEnrollmentCount(c.getClassId()) : 0;
            String capInfo = c != null ? (enrolledCount + "/" + c.getCapacity()) : "";
            tableModel.addRow(new Object[]{
                    e.getEnrollmentId(),
                    e.getEnrollmentCode(),
                    s != null ? s.getStudentCode() : "",
                    s != null ? s.getFullName() : "",
                    c != null ? c.getClassCode() : "",
                    course != null ? course.getCourseName() : "",
                    capInfo,
                    e.getEnrollmentDate(),
                    tf != null ? FormatUtil.formatVnd(tf.getAmountDue()) : "0 VNĐ",
                    e.getStatus(),
                    e.getCancelReason()
            });
        }
    }
}

