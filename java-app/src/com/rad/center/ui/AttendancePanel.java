package com.rad.center.ui;

import com.rad.center.dao.CenterRepository;
import com.rad.center.model.*;
import com.rad.center.service.CenterService;
import com.rad.center.service.ValidationException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Chức năng 5 – Quản lý Buổi học & Điểm danh Học viên (FR-12, FR-13, FR-14, UC-06, BR-20 -> BR-25, TC-08, TC-09, TC-10)
 */
public class AttendancePanel extends JPanel {
    private final CenterService service;
    private final CenterRepository repo = CenterRepository.getInstance();
    private final Runnable onDataChanged;

    private final JComboBox<ClassRoom> cboClass = new JComboBox<ClassRoom>();
    private final JComboBox<Session> cboSession = new JComboBox<Session>();
    private final JTextField txtNewTopic = UITheme.createTextField(16);
    private final JTextField txtNewDate = UITheme.createTextField(9);
    private final JLabel lblAttendanceSummary = new JLabel("Chọn lớp và buổi học để điểm danh...");

    private final DefaultTableModel attendanceModel;
    private final JTable attendanceTable;

    public AttendancePanel(final CenterService service, final Runnable onDataChanged) {
        this.service = service;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_LIGHT);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // Top Filter & Session Control Card
        JPanel topCard = UITheme.createCardPanel("Chức năng 5: Quản lý Buổi học & Điểm danh Học viên theo Buổi (UC-06 | BR-21, BR-22: Chống tạo bản ghi trùng)");
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row1.setOpaque(false);
        row1.add(new JLabel("Lớp (*):"));
        cboClass.setPreferredSize(new Dimension(175, 30));
        row1.add(cboClass);
        row1.add(new JLabel("Buổi học (*):"));
        cboSession.setPreferredSize(new Dimension(215, 30));
        row1.add(cboSession);

        JButton btnMarkAllPresent = UITheme.createButton("Đặt tất cả: PRESENT", new Color(100, 116, 139), Color.WHITE);
        JButton btnSaveAttendance = UITheme.createButton("Lưu Điểm danh (FR-13)", UITheme.SUCCESS, Color.WHITE);
        row1.add(btnMarkAllPresent);
        row1.add(btnSaveAttendance);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        row2.setOpaque(false);
        txtNewTopic.setText("Buổi 4: Luyện đề Listening & Reading Tổng hợp");
        txtNewDate.setText("2026-11-09");
        row2.add(new JLabel("Tạo buổi học mới (FR-12) - Chủ đề:"));
        row2.add(txtNewTopic);
        row2.add(new JLabel("Ngày (YYYY-MM-DD):"));
        row2.add(txtNewDate);
        JButton btnAddSession = UITheme.createButton("+ Thêm buổi học cho lớp", UITheme.PRIMARY, Color.WHITE);
        row2.add(btnAddSession);

        topCard.add(row1, BorderLayout.CENTER);
        topCard.add(row2, BorderLayout.SOUTH);
        add(topCard, BorderLayout.NORTH);

        // Center Table
        JPanel tableCard = UITheme.createCardPanel("Danh sách Học viên có Đăng ký Hợp lệ (CONFIRMED) trong Lớp & Trạng thái Điểm danh");
        attendanceModel = new DefaultTableModel(new Object[]{
                "ID Học viên", "Mã HV", "Họ và tên Học viên", "Số điện thoại", "Trạng thái Điểm danh (*)", "Ghi chú buổi học", "Người điểm danh", "Thời điểm lưu", "Tỷ lệ Chuyên cần Lớp (FR-14)"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 4 || c == 5; // Cho phép sửa cột Trạng thái và Ghi chú
            }
        };
        attendanceTable = new JTable(attendanceModel);
        UITheme.styleTable(attendanceTable);

        // ComboBox Editor cho cột Trạng thái Điểm danh (PRESENT, ABSENT_EXCUSED, ABSENT_UNEXCUSED)
        TableColumn statusCol = attendanceTable.getColumnModel().getColumn(4);
        JComboBox<String> statusCombo = new JComboBox<String>(new String[]{
                "PRESENT", "ABSENT_EXCUSED", "ABSENT_UNEXCUSED"
        });
        statusCol.setCellEditor(new DefaultCellEditor(statusCombo));

        lblAttendanceSummary.setFont(UITheme.FONT_BOLD);
        lblAttendanceSummary.setForeground(UITheme.PRIMARY);
        lblAttendanceSummary.setBorder(new EmptyBorder(6, 4, 2, 4));

        tableCard.add(new JScrollPane(attendanceTable), BorderLayout.CENTER);
        tableCard.add(lblAttendanceSummary, BorderLayout.SOUTH);
        add(tableCard, BorderLayout.CENTER);

        cboClass.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadSessionsForSelectedClass();
            }
        });

        cboSession.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadAttendanceTable();
            }
        });

        btnMarkAllPresent.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < attendanceModel.getRowCount(); i++) {
                    attendanceModel.setValueAt("PRESENT", i, 4);
                }
            }
        });

        btnSaveAttendance.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (attendanceTable.isEditing()) {
                    attendanceTable.getCellEditor().stopCellEditing();
                }
                Session session = (Session) cboSession.getSelectedItem();
                if (session == null) {
                    JOptionPane.showMessageDialog(AttendancePanel.this, "Vui lòng chọn buổi học cần lưu điểm danh!");
                    return;
                }
                try {
                    int count = 0;
                    for (int i = 0; i < attendanceModel.getRowCount(); i++) {
                        int studentId = (Integer) attendanceModel.getValueAt(i, 0);
                        String status = (String) attendanceModel.getValueAt(i, 4);
                        String note = (String) attendanceModel.getValueAt(i, 5);
                        service.markAttendance(session.getSessionId(), studentId, status, note);
                        count++;
                    }
                    loadAttendanceTable();
                    if (onDataChanged != null) onDataChanged.run();
                    JOptionPane.showMessageDialog(AttendancePanel.this,
                            "Đã lưu kết quả điểm danh cho " + count + " học viên của " + session.toString() + "!\n"
                                    + "(BR-22 / TC-09: Tự động cập nhật trên cặp khóa [session_id, student_id], không tạo bản ghi trùng)",
                            "Lưu điểm danh thành công (TC-08 / TC-09)", JOptionPane.INFORMATION_MESSAGE);
                } catch (ValidationException ex) {
                    JOptionPane.showMessageDialog(AttendancePanel.this,
                            "[" + ex.getRuleCode() + "] " + ex.getMessage(),
                            "Lỗi điểm danh", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnAddSession.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ClassRoom cl = (ClassRoom) cboClass.getSelectedItem();
                if (cl == null) return;
                try {
                    Session created = service.addSession(
                            cl.getClassId(),
                            txtNewTopic.getText(),
                            txtNewDate.getText(),
                            "18:00",
                            "20:00"
                    );
                    loadSessionsForSelectedClass();
                    cboSession.setSelectedItem(created);
                    if (onDataChanged != null) onDataChanged.run();
                    JOptionPane.showMessageDialog(AttendancePanel.this,
                            "Đã thêm buổi học mới: " + created.toString(),
                            "Thành công (FR-12)", JOptionPane.INFORMATION_MESSAGE);
                } catch (ValidationException ex) {
                    JOptionPane.showMessageDialog(AttendancePanel.this, ex.getMessage(), "Lỗi tạo buổi học", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        refreshData();
    }

    private void loadSessionsForSelectedClass() {
        cboSession.removeAllItems();
        ClassRoom cl = (ClassRoom) cboClass.getSelectedItem();
        if (cl == null) return;
        List<Session> sessions = service.getSessionsByClass(cl.getClassId());
        for (Session s : sessions) {
            cboSession.addItem(s);
        }
        loadAttendanceTable();
    }

    private void loadAttendanceTable() {
        attendanceModel.setRowCount(0);
        ClassRoom cl = (ClassRoom) cboClass.getSelectedItem();
        Session session = (Session) cboSession.getSelectedItem();
        if (cl == null || session == null) {
            lblAttendanceSummary.setText("Chưa có buổi học nào được chọn.");
            return;
        }
        List<Student> students = service.getEligibleStudentsForClass(cl.getClassId());
        int present = 0, excused = 0, unexcused = 0;
        for (Student s : students) {
            Attendance rec = service.getAttendanceRecord(session.getSessionId(), s.getStudentId());
            String st = rec != null ? rec.getStatus() : "PRESENT";
            String note = rec != null ? rec.getNote() : "";
            String marker = rec != null ? rec.getMarkedBy() : "(Chưa lưu)";
            String markedAt = rec != null ? rec.getMarkedAt() : "-";
            if ("PRESENT".equals(st)) present++;
            else if ("ABSENT_EXCUSED".equals(st)) excused++;
            else unexcused++;

            double rate = service.calculateStudentAttendanceRate(cl.getClassId(), s.getStudentId());
            attendanceModel.addRow(new Object[]{
                    s.getStudentId(),
                    s.getStudentCode(),
                    s.getFullName(),
                    s.getPhone(),
                    st,
                    note,
                    marker,
                    markedAt,
                    String.format("%.0f%%", rate)
            });
        }
        lblAttendanceSummary.setText(String.format(
                "Tổng hợp buổi học [%s]: Sĩ số hợp lệ: %d HV | Có mặt (PRESENT): %d | Vắng có phép (ABSENT_EXCUSED): %d | Vắng không phép (ABSENT_UNEXCUSED): %d",
                session.getTopic(), students.size(), present, excused, unexcused
        ));
    }

    public void refreshData() {
        ClassRoom prevClass = (ClassRoom) cboClass.getSelectedItem();
        cboClass.removeAllItems();
        for (ClassRoom cl : repo.getClasses()) {
            cboClass.addItem(cl);
        }
        if (prevClass != null) {
            for (int i = 0; i < cboClass.getItemCount(); i++) {
                if (cboClass.getItemAt(i).getClassId() == prevClass.getClassId()) {
                    cboClass.setSelectedIndex(i);
                    break;
                }
            }
        }
        loadSessionsForSelectedClass();
    }
}
