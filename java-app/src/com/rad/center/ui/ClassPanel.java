package com.rad.center.ui;

import com.rad.center.dao.CenterRepository;
import com.rad.center.model.ClassRoom;
import com.rad.center.model.Course;
import com.rad.center.model.Teacher;
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

/**
 * Chức năng 2 – Quản lý lớp học & phân công giảng viên (FR-04, FR-05, UC-03, BR-04 -> BR-07)
 */
public class ClassPanel extends JPanel {
    private final CenterService service;
    private final CenterRepository repo = CenterRepository.getInstance();
    private final Runnable onDataChanged;

    private final JTextField txtClassCode = UITheme.createTextField(10);
    private final JComboBox<Course> cboCourse = new JComboBox<Course>();
    private final JComboBox<Teacher> cboTeacher = new JComboBox<Teacher>();
    private final JTextField txtCapacity = UITheme.createTextField(8);
    private final JTextField txtStartDate = UITheme.createTextField(10);
    private final JTextField txtEndDate = UITheme.createTextField(10);
    private final JTextField txtSchedule = UITheme.createTextField(18);
    private final JTextField txtRoom = UITheme.createTextField(8);
    private final JComboBox<String> cboStatus = new JComboBox<String>(new String[]{"OPEN", "ONGOING", "CLOSED", "CANCELLED"});

    private final DefaultTableModel tableModel;
    private final JTable table;
    private int selectedClassId = -1;

    public ClassPanel(final CenterService service, final Runnable onDataChanged) {
        this.service = service;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_LIGHT);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel formCard = UITheme.createCardPanel("Chức năng 2: Quản lý Lớp học, Sĩ số & Phân công Giảng viên (FR-04, FR-05 | BR-05, BR-06, BR-07)");
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtClassCode.setText("TOEIC03");
        txtCapacity.setText("20");
        txtStartDate.setText("2026-11-15");
        txtEndDate.setText("2027-02-15");
        txtSchedule.setText("Thứ 3, 5, 7 (19:30 - 21:30)");
        txtRoom.setText("P.203");

        addField(grid, gbc, 0, 0, "Mã lớp học (*):", txtClassCode);
        addField(grid, gbc, 2, 0, "Khóa học trực thuộc (*):", cboCourse);
        addField(grid, gbc, 4, 0, "Giảng viên phụ trách (*):", cboTeacher);

        addField(grid, gbc, 0, 1, "Sĩ số tối đa (>0):", txtCapacity);
        addField(grid, gbc, 2, 1, "Lịch học trong tuần:", txtSchedule);
        addField(grid, gbc, 4, 1, "Phòng học:", txtRoom);

        addField(grid, gbc, 0, 2, "Ngày khai giảng:", txtStartDate);
        addField(grid, gbc, 2, 2, "Ngày kết thúc:", txtEndDate);
        addField(grid, gbc, 4, 2, "Trạng thái lớp:", cboStatus);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        actions.setOpaque(false);
        JButton btnAdd = UITheme.createButton("+ Mở lớp học mới", UITheme.PRIMARY, Color.WHITE);
        JButton btnUpdate = UITheme.createButton("Lưu cập nhật lớp", UITheme.SUCCESS, Color.WHITE);
        actions.add(btnAdd);
        actions.add(btnUpdate);

        formCard.add(grid, BorderLayout.CENTER);
        formCard.add(actions, BorderLayout.SOUTH);
        add(formCard, BorderLayout.NORTH);

        JPanel tableCard = UITheme.createCardPanel("Danh sách Lớp học, Lịch học, Giảng viên & Kiểm soát Sĩ số Tập trung");
        tableModel = new DefaultTableModel(new Object[]{
                "ID", "Mã Lớp", "Khóa học", "Học phí chuẩn", "Giảng viên phụ trách", "Lịch học", "Phòng", "Sĩ số (Đã ĐK / Tối đa)", "Còn trống", "Trạng thái"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                    int r = table.getSelectedRow();
                    selectedClassId = (Integer) tableModel.getValueAt(r, 0);
                    ClassRoom cl = repo.findClassById(selectedClassId);
                    if (cl != null) {
                        txtClassCode.setText(cl.getClassCode());
                        txtCapacity.setText(String.valueOf(cl.getCapacity()));
                        txtStartDate.setText(cl.getStartDate());
                        txtEndDate.setText(cl.getEndDate());
                        txtSchedule.setText(cl.getScheduleSummary());
                        txtRoom.setText(cl.getRoom());
                        cboStatus.setSelectedItem(cl.getStatus());
                        for (int i = 0; i < cboCourse.getItemCount(); i++) {
                            if (cboCourse.getItemAt(i).getCourseId() == cl.getCourseId()) {
                                cboCourse.setSelectedIndex(i);
                                break;
                            }
                        }
                        for (int i = 0; i < cboTeacher.getItemCount(); i++) {
                            if (cboTeacher.getItemAt(i).getTeacherId() == cl.getTeacherId()) {
                                cboTeacher.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                }
            }
        });

        btnAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Course c = (Course) cboCourse.getSelectedItem();
                    Teacher t = (Teacher) cboTeacher.getSelectedItem();
                    int cap = Integer.parseInt(txtCapacity.getText().trim());
                    ClassRoom created = service.addClassRoom(
                            txtClassCode.getText(),
                            c != null ? c.getCourseId() : 1,
                            t != null ? t.getTeacherId() : 1,
                            cap,
                            txtStartDate.getText(),
                            txtEndDate.getText(),
                            txtSchedule.getText(),
                            txtRoom.getText(),
                            (String) cboStatus.getSelectedItem()
                    );
                    refreshData();
                    if (onDataChanged != null) onDataChanged.run();
                    JOptionPane.showMessageDialog(ClassPanel.this,
                            "Đã tạo lớp học " + created.getClassCode() + " thành công!",
                            "Thành công (FR-04)", JOptionPane.INFORMATION_MESSAGE);
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(ClassPanel.this, "Sĩ số tối đa phải là số nguyên dương!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } catch (ValidationException ex) {
                    JOptionPane.showMessageDialog(ClassPanel.this, "[" + ex.getRuleCode() + "] " + ex.getMessage(), "Lỗi quy tắc nghiệp vụ", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnUpdate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedClassId < 0) return;
                try {
                    Course c = (Course) cboCourse.getSelectedItem();
                    Teacher t = (Teacher) cboTeacher.getSelectedItem();
                    int cap = Integer.parseInt(txtCapacity.getText().trim());
                    ClassRoom updated = service.updateClassRoom(
                            selectedClassId,
                            c != null ? c.getCourseId() : 1,
                            t != null ? t.getTeacherId() : 1,
                            cap,
                            txtStartDate.getText(),
                            txtEndDate.getText(),
                            txtSchedule.getText(),
                            txtRoom.getText(),
                            (String) cboStatus.getSelectedItem()
                    );
                    refreshData();
                    if (onDataChanged != null) onDataChanged.run();
                    JOptionPane.showMessageDialog(ClassPanel.this,
                            "Đã cập nhật lớp học " + updated.getClassCode() + "!",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ClassPanel.this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        refreshData();
        if (tableModel.getRowCount() > 0) table.setRowSelectionInterval(0, 0);
    }

    private void addField(JPanel p, GridBagConstraints gbc, int x, int y, String label, JComponent c) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.weightx = 0;
        JLabel l = new JLabel(label);
        l.setFont(UITheme.FONT_BOLD);
        p.add(l, gbc);
        gbc.gridx = x + 1;
        gbc.weightx = 1.0;
        p.add(c, gbc);
    }

    public void refreshData() {
        cboCourse.removeAllItems();
        for (Course c : repo.getCourses()) cboCourse.addItem(c);

        cboTeacher.removeAllItems();
        for (Teacher t : repo.getTeachers()) cboTeacher.addItem(t);

        tableModel.setRowCount(0);
        for (ClassRoom cl : repo.getClasses()) {
            Course course = repo.findCourseById(cl.getCourseId());
            Teacher teacher = repo.findTeacherById(cl.getTeacherId());
            int enrolled = service.getConfirmedEnrollmentCount(cl.getClassId());
            int remaining = Math.max(0, cl.getCapacity() - enrolled);
            tableModel.addRow(new Object[]{
                    cl.getClassId(),
                    cl.getClassCode(),
                    course != null ? course.getCourseName() : "",
                    course != null ? FormatUtil.formatVnd(course.getTuition()) : "",
                    teacher != null ? teacher.getFullName() : "",
                    cl.getScheduleSummary(),
                    cl.getRoom(),
                    enrolled + " / " + cl.getCapacity(),
                    remaining == 0 ? "0 (ĐẦY SĨ SỐ)" : remaining + " chỗ",
                    cl.getStatus()
            });
        }
    }
}

