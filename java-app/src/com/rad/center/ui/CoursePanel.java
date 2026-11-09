package com.rad.center.ui;

import com.rad.center.dao.CenterRepository;
import com.rad.center.model.ClassRoom;
import com.rad.center.model.Course;
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
 * Màn hình Quản lý Khóa học (FR-03, BR-03, BR-04)
 */
public class CoursePanel extends JPanel {
    private final CenterService service;
    private final CenterRepository repo = CenterRepository.getInstance();
    private final Runnable onDataChanged;

    private final JTextField txtCode = UITheme.createTextField(12);
    private final JTextField txtName = UITheme.createTextField(20);
    private final JTextField txtDuration = UITheme.createTextField(14);
    private final JTextField txtTuition = UITheme.createTextField(12);
    private final JTextField txtEntryLevel = UITheme.createTextField(14);
    private final JTextField txtDescription = UITheme.createTextField(25);
    private final JComboBox<String> cboStatus = new JComboBox<String>(new String[]{"ACTIVE", "INACTIVE"});

    private final DefaultTableModel tableModel;
    private final JTable table;
    private int selectedCourseId = -1;

    public CoursePanel(final CenterService service, final Runnable onDataChanged) {
        this.service = service;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_LIGHT);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel formCard = UITheme.createCardPanel("Quản lý Chương trình / Khóa học Đào tạo (FR-03 | BR-03: Một khóa học có thể mở nhiều lớp học)");
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtCode.setText("KH-IELTS65");
        txtDuration.setText("3 tháng (36 buổi)");
        txtTuition.setText("4500000");
        txtEntryLevel.setText("IELTS 5.0+");

        addField(grid, gbc, 0, 0, "Mã khóa học (*):", txtCode);
        addField(grid, gbc, 2, 0, "Tên khóa học (*):", txtName);
        addField(grid, gbc, 4, 0, "Thời lượng:", txtDuration);
        addField(grid, gbc, 0, 1, "Học phí chuẩn (VNĐ):", txtTuition);
        addField(grid, gbc, 2, 1, "Đầu vào:", txtEntryLevel);
        addField(grid, gbc, 4, 1, "Trạng thái:", cboStatus);
        addField(grid, gbc, 0, 2, "Mô tả nội dung:", txtDescription);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        actions.setOpaque(false);
        JButton btnAdd = UITheme.createButton("+ Thêm khóa học", UITheme.PRIMARY, Color.WHITE);
        JButton btnUpdate = UITheme.createButton("Cập nhật khóa học", UITheme.SUCCESS, Color.WHITE);
        actions.add(btnAdd);
        actions.add(btnUpdate);

        formCard.add(grid, BorderLayout.CENTER);
        formCard.add(actions, BorderLayout.SOUTH);
        add(formCard, BorderLayout.NORTH);

        JPanel tableCard = UITheme.createCardPanel("Danh mục Khóa học hiện có tại Trung tâm");
        tableModel = new DefaultTableModel(new Object[]{
                "ID", "Mã Khóa học", "Tên Khóa học", "Thời lượng", "Học phí Mặc định", "Trình độ Đầu vào", "Số Lớp đã mở", "Trạng thái"
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
                    selectedCourseId = (Integer) tableModel.getValueAt(r, 0);
                    Course c = repo.findCourseById(selectedCourseId);
                    if (c != null) {
                        txtCode.setText(c.getCourseCode());
                        txtName.setText(c.getCourseName());
                        txtDuration.setText(c.getDuration());
                        txtTuition.setText(String.valueOf(c.getTuition()));
                        txtEntryLevel.setText(c.getEntryLevel());
                        txtDescription.setText(c.getDescription());
                        cboStatus.setSelectedItem(c.getStatus());
                    }
                }
            }
        });

        btnAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    long tuition = Long.parseLong(FormatUtil.parseCleanDigits(txtTuition.getText()));
                    Course created = service.addCourse(
                            txtCode.getText(), txtName.getText(), txtDescription.getText(),
                            txtDuration.getText(), tuition, txtEntryLevel.getText(),
                            (String) cboStatus.getSelectedItem()
                    );
                    refreshData();
                    if (onDataChanged != null) onDataChanged.run();
                    JOptionPane.showMessageDialog(CoursePanel.this, "Đã tạo khóa học: " + created.getCourseName());
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(CoursePanel.this, "Học phí phải là số nguyên hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } catch (ValidationException ex) {
                    JOptionPane.showMessageDialog(CoursePanel.this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnUpdate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedCourseId < 0) return;
                try {
                    long tuition = Long.parseLong(FormatUtil.parseCleanDigits(txtTuition.getText()));
                    service.updateCourse(
                            selectedCourseId, txtName.getText(), txtDescription.getText(),
                            txtDuration.getText(), tuition, txtEntryLevel.getText(),
                            (String) cboStatus.getSelectedItem()
                    );
                    refreshData();
                    if (onDataChanged != null) onDataChanged.run();
                    JOptionPane.showMessageDialog(CoursePanel.this, "Cập nhật khóa học thành công!");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CoursePanel.this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
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
        tableModel.setRowCount(0);
        for (Course c : repo.getCourses()) {
            int classCount = 0;
            for (ClassRoom cl : repo.getClasses()) {
                if (cl.getCourseId() == c.getCourseId()) classCount++;
            }
            tableModel.addRow(new Object[]{
                    c.getCourseId(),
                    c.getCourseCode(),
                    c.getCourseName(),
                    c.getDuration(),
                    FormatUtil.formatVnd(c.getTuition()),
                    c.getEntryLevel(),
                    classCount + " lớp",
                    c.getStatus()
            });
        }
    }
}

