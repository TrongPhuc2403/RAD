package com.rad.center.ui;

import com.rad.center.dao.CenterRepository;
import com.rad.center.model.*;
import com.rad.center.service.CenterService;
import com.rad.center.service.ValidationException;
import com.rad.center.util.FormatUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Chức năng 1 – Quản lý học viên (FR-02, UC-02, BR-01, BR-08, TC-01, TC-02)
 */
public class StudentPanel extends JPanel {
    private final CenterService service;
    private final CenterRepository repo = CenterRepository.getInstance();
    private final Runnable onDataChanged;

    private final JTextField txtCode = UITheme.createTextField(10);
    private final JTextField txtFullName = UITheme.createTextField(18);
    private final JTextField txtDob = UITheme.createTextField(10);
    private final JComboBox<String> cboGender = new JComboBox<String>(new String[]{"Nam", "Nữ", "Khác"});
    private final JTextField txtPhone = UITheme.createTextField(12);
    private final JTextField txtEmail = UITheme.createTextField(18);
    private final JTextField txtAddress = UITheme.createTextField(20);
    private final JComboBox<String> cboStatus = new JComboBox<String>(new String[]{"ACTIVE", "INACTIVE"});
    private final JTextField txtSearch = UITheme.createTextField(22);

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final DefaultTableModel historyModel;
    private int selectedStudentId = -1;

    public StudentPanel(final CenterService service, final Runnable onDataChanged) {
        this.service = service;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_LIGHT);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // 1. Form Card (Top)
        JPanel formCard = UITheme.createCardPanel("Chức năng 1: Quản lý Hồ sơ Học viên (FR-02 | BR-01: Mã duy nhất, Không xóa cứng khi đã có đăng ký)");
        JPanel formFields = new JPanel(new GridBagLayout());
        formFields.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtCode.setText(service.generateNextStudentCode());
        txtDob.setText("2004-06-15");

        addFormField(formFields, gbc, 0, 0, "Mã học viên (*):", txtCode);
        addFormField(formFields, gbc, 2, 0, "Họ và tên (*):", txtFullName);
        addFormField(formFields, gbc, 4, 0, "Ngày sinh (YYYY-MM-DD):", txtDob);
        addFormField(formFields, gbc, 6, 0, "Giới tính:", cboGender);

        addFormField(formFields, gbc, 0, 1, "Số điện thoại (*):", txtPhone);
        addFormField(formFields, gbc, 2, 1, "Email (*):", txtEmail);
        addFormField(formFields, gbc, 4, 1, "Địa chỉ:", txtAddress);
        addFormField(formFields, gbc, 6, 1, "Trạng thái hồ sơ:", cboStatus);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        actionRow.setOpaque(false);
        JButton btnAdd = UITheme.createButton("+ Thêm học viên mới", UITheme.PRIMARY, Color.WHITE);
        JButton btnUpdate = UITheme.createButton("Lưu cập nhật", UITheme.SUCCESS, Color.WHITE);
        JButton btnClear = UITheme.createButton("Làm mới form", new Color(100, 116, 139), Color.WHITE);

        actionRow.add(btnAdd);
        actionRow.add(btnUpdate);
        actionRow.add(btnClear);
        actionRow.add(Box.createHorizontalStrut(20));
        actionRow.add(new JLabel("Tìm kiếm (Mã / Tên / SĐT):"));
        actionRow.add(txtSearch);
        JButton btnSearch = UITheme.createButton("Tìm kiếm", new Color(15, 23, 42), Color.WHITE);
        actionRow.add(btnSearch);

        formCard.add(formFields, BorderLayout.CENTER);
        formCard.add(actionRow, BorderLayout.SOUTH);
        add(formCard, BorderLayout.NORTH);

        // 2. Split Center: Student List Table + Enrollment/Tuition History of Selected Student
        JPanel centerSplit = new JPanel(new BorderLayout(12, 12));
        centerSplit.setOpaque(false);

        JPanel listCard = UITheme.createCardPanel("Danh sách Hồ sơ Học viên trên hệ thống");
        tableModel = new DefaultTableModel(new Object[]{
                "ID", "Mã HV", "Họ và tên", "Ngày sinh", "Giới tính", "Số điện thoại", "Email", "Địa chỉ", "Trạng thái", "Ngày tạo"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        listCard.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel historyCard = UITheme.createCardPanel("Lịch sử Đăng ký Lớp & Nghĩa vụ Học phí của Học viên đang chọn (Truy vết dữ liệu liên kết)");
        historyCard.setPreferredSize(new Dimension(100, 175));
        historyModel = new DefaultTableModel(new Object[]{
                "Mã ĐK", "Mã Lớp", "Khóa học", "Ngày đăng ký", "Trạng thái ĐK", "Học phí phải thu", "Đã đóng", "Còn nợ", "Tỷ lệ chuyên cần"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable historyTable = new JTable(historyModel);
        UITheme.styleTable(historyTable);
        historyCard.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        centerSplit.add(listCard, BorderLayout.CENTER);
        centerSplit.add(historyCard, BorderLayout.SOUTH);
        add(centerSplit, BorderLayout.CENTER);

        // Event Listeners
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                    int row = table.getSelectedRow();
                    selectedStudentId = (Integer) tableModel.getValueAt(row, 0);
                    Student s = repo.findStudentById(selectedStudentId);
                    if (s != null) {
                        txtCode.setText(s.getStudentCode());
                        txtFullName.setText(s.getFullName());
                        txtDob.setText(s.getDob());
                        cboGender.setSelectedItem(s.getGender());
                        txtPhone.setText(s.getPhone());
                        txtEmail.setText(s.getEmail());
                        txtAddress.setText(s.getAddress());
                        cboStatus.setSelectedItem(s.getStatus());
                        loadStudentHistory(s.getStudentId());
                    }
                }
            }
        });

        btnAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Student created = service.addStudent(
                            txtCode.getText(),
                            txtFullName.getText(),
                            txtDob.getText(),
                            (String) cboGender.getSelectedItem(),
                            txtPhone.getText(),
                            txtEmail.getText(),
                            txtAddress.getText()
                    );
                    refreshData();
                    if (onDataChanged != null) onDataChanged.run();
                    clearForm();
                    JOptionPane.showMessageDialog(StudentPanel.this,
                            "Thêm học viên thành công: " + created.getStudentCode() + " - " + created.getFullName(),
                            "Thành công (TC-01)", JOptionPane.INFORMATION_MESSAGE);
                } catch (ValidationException ex) {
                    JOptionPane.showMessageDialog(StudentPanel.this,
                            "[" + ex.getRuleCode() + "] " + ex.getMessage(),
                            "Lỗi kiểm tra dữ liệu (TC-02)", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnUpdate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedStudentId < 0) {
                    JOptionPane.showMessageDialog(StudentPanel.this, "Vui lòng chọn một học viên trong bảng để cập nhật!");
                    return;
                }
                try {
                    Student updated = service.updateStudent(
                            selectedStudentId,
                            txtFullName.getText(),
                            txtDob.getText(),
                            (String) cboGender.getSelectedItem(),
                            txtPhone.getText(),
                            txtEmail.getText(),
                            txtAddress.getText(),
                            (String) cboStatus.getSelectedItem()
                    );
                    refreshData();
                    if (onDataChanged != null) onDataChanged.run();
                    JOptionPane.showMessageDialog(StudentPanel.this,
                            "Đã cập nhật hồ sơ học viên: " + updated.getFullName(),
                            "Cập nhật thành công", JOptionPane.INFORMATION_MESSAGE);
                } catch (ValidationException ex) {
                    JOptionPane.showMessageDialog(StudentPanel.this,
                            "[" + ex.getRuleCode() + "] " + ex.getMessage(),
                            "Lỗi cập nhật", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearForm();
            }
        });

        btnSearch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshData();
            }
        });

        refreshData();
        if (tableModel.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int x, int y, String label, JComponent comp) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_BOLD);
        panel.add(lbl, gbc);

        gbc.gridx = x + 1;
        gbc.weightx = 1.0;
        panel.add(comp, gbc);
    }

    private void clearForm() {
        selectedStudentId = -1;
        table.clearSelection();
        txtCode.setText(service.generateNextStudentCode());
        txtFullName.setText("");
        txtDob.setText("2004-06-15");
        cboGender.setSelectedIndex(0);
        txtPhone.setText("");
        txtEmail.setText("");
        txtAddress.setText("");
        cboStatus.setSelectedIndex(0);
        historyModel.setRowCount(0);
    }

    private void loadStudentHistory(int studentId) {
        historyModel.setRowCount(0);
        for (Enrollment e : repo.getEnrollments()) {
            if (e.getStudentId() == studentId) {
                ClassRoom c = repo.findClassById(e.getClassId());
                Course course = c != null ? repo.findCourseById(c.getCourseId()) : null;
                TuitionFee tf = repo.findTuitionByEnrollmentId(e.getEnrollmentId());
                long due = tf != null ? tf.getAmountDue() : 0L;
                long paid = tf != null ? service.getTotalPaidForTuition(tf.getTuitionId()) : 0L;
                long debt = tf != null ? service.getRemainingDebt(tf.getTuitionId()) : 0L;
                double rate = c != null ? service.calculateStudentAttendanceRate(c.getClassId(), studentId) : 0.0;
                historyModel.addRow(new Object[]{
                        e.getEnrollmentCode(),
                        c != null ? c.getClassCode() : "",
                        course != null ? course.getCourseName() : "",
                        e.getEnrollmentDate(),
                        e.getStatus(),
                        FormatUtil.formatVnd(due),
                        FormatUtil.formatVnd(paid),
                        FormatUtil.formatVnd(debt),
                        String.format("%.0f%%", rate)
                });
            }
        }
    }

    public void refreshData() {
        List<Student> list = service.searchStudents(txtSearch.getText());
        tableModel.setRowCount(0);
        for (Student s : list) {
            tableModel.addRow(new Object[]{
                    s.getStudentId(),
                    s.getStudentCode(),
                    s.getFullName(),
                    s.getDob(),
                    s.getGender(),
                    s.getPhone(),
                    s.getEmail(),
                    s.getAddress(),
                    s.getStatus(),
                    s.getCreatedAt()
            });
        }
        if (selectedStudentId > 0) {
            loadStudentHistory(selectedStudentId);
        }
    }
}

