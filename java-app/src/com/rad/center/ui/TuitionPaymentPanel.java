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
 * Chức năng 4 – Quản lý Học phí & Thanh toán Học phí (FR-10, FR-11, UC-05, BR-16 -> BR-19, TC-06, TC-07)
 */
public class TuitionPaymentPanel extends JPanel {
    private final CenterService service;
    private final CenterRepository repo = CenterRepository.getInstance();
    private final Runnable onDataChanged;

    private final JLabel lblSelectedInfo = new JLabel("Chọn một nghĩa vụ học phí ở bảng bên trái để thu tiền...");
    private final JLabel lblAmountDue = new JLabel("0 VNĐ");
    private final JLabel lblTotalPaid = new JLabel("0 VNĐ");
    private final JLabel lblRemainingDebt = new JLabel("0 VNĐ");

    private final JTextField txtPayAmount = UITheme.createTextField(9);
    private final JComboBox<String> cboMethod = new JComboBox<String>(new String[]{"CASH", "TRANSFER", "CARD"});
    private final JTextField txtNote = UITheme.createTextField(12);
    private final JTextField txtFilter = UITheme.createTextField(10);

    private final DefaultTableModel tuitionModel;
    private final JTable tuitionTable;
    private final DefaultTableModel paymentHistoryModel;
    private int selectedTuitionId = -1;

    public TuitionPaymentPanel(final CenterService service, final Runnable onDataChanged) {
        this.service = service;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(14, 14));
        setBackground(UITheme.BG_LIGHT);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // 1. Top Payment Form Card
        JPanel payCard = UITheme.createCardPanel("Chức năng 4: Ghi nhận Giao dịch Thanh toán & Theo dõi Công nợ (UC-05 | BR-19: Còn nợ = Phải thu - Đã thanh toán)");
        JPanel summaryRow = new JPanel(new GridLayout(1, 4, 12, 0));
        summaryRow.setOpaque(false);

        summaryRow.add(createInfoBox("Khoản đăng ký đang chọn:", lblSelectedInfo, UITheme.PRIMARY));
        summaryRow.add(createInfoBox("Tổng học phí phải thu (BR-15):", lblAmountDue, UITheme.TEXT_DARK));
        summaryRow.add(createInfoBox("Tổng tiền đã đóng (BR-16):", lblTotalPaid, UITheme.SUCCESS));
        summaryRow.add(createInfoBox("Số tiền còn nợ (BR-19):", lblRemainingDebt, UITheme.DANGER));

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        inputRow.setOpaque(false);
        txtPayAmount.setText("500000");
        txtNote.setText("Thu học phí đợt tiếp theo");

        inputRow.add(new JLabel("Số tiền thu (VNĐ) (*):"));
        inputRow.add(txtPayAmount);
        inputRow.add(new JLabel("Phương thức:"));
        inputRow.add(cboMethod);
        inputRow.add(new JLabel("Ghi chú:"));
        inputRow.add(txtNote);

        JButton btnPay = UITheme.createButton("+ Xác nhận Thu Học phí (FR-10)", UITheme.SUCCESS, Color.WHITE);
        JButton btnPayFull = UITheme.createButton("Điền số nợ còn lại", UITheme.PRIMARY, Color.WHITE);
        inputRow.add(btnPay);
        inputRow.add(btnPayFull);

        inputRow.add(Box.createHorizontalStrut(15));
        inputRow.add(new JLabel("Tra cứu HV/Lớp:"));
        inputRow.add(txtFilter);
        JButton btnSearch = UITheme.createButton("Lọc công nợ", new Color(15, 23, 42), Color.WHITE);
        inputRow.add(btnSearch);

        payCard.add(summaryRow, BorderLayout.CENTER);
        payCard.add(inputRow, BorderLayout.SOUTH);
        add(payCard, BorderLayout.NORTH);

        // 2. Center Split: Tuition Obligations Table + Payment History Table
        JPanel split = new JPanel(new BorderLayout(12, 12));
        split.setOpaque(false);

        JPanel tuitionListCard = UITheme.createCardPanel("Danh sách Nghĩa vụ Học phí & Tình trạng Công nợ theo từng Đăng ký (FR-11)");
        tuitionModel = new DefaultTableModel(new Object[]{
                "Mã HP", "Mã ĐK", "Mã HV", "Họ và tên Học viên", "Lớp học", "Khóa học", "Học phí Phải thu", "Đã thanh toán", "Còn nợ", "Trạng thái HP"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tuitionTable = new JTable(tuitionModel);
        UITheme.styleTable(tuitionTable);
        tuitionListCard.add(new JScrollPane(tuitionTable), BorderLayout.CENTER);

        JPanel historyCard = UITheme.createCardPanel("Lịch sử Giao dịch Thanh toán nhiều lần của Khoản Học phí đang chọn (BR-16, TC-06)");
        historyCard.setPreferredSize(new Dimension(100, 185));
        paymentHistoryModel = new DefaultTableModel(new Object[]{
                "Mã Giao dịch", "Mã HP", "Số tiền Thanh toán", "Ngày thanh toán", "Phương thức", "Thu ngân tiếp nhận", "Ghi chú"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable historyTable = new JTable(paymentHistoryModel);
        UITheme.styleTable(historyTable);
        historyCard.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        split.add(tuitionListCard, BorderLayout.CENTER);
        split.add(historyCard, BorderLayout.SOUTH);
        add(split, BorderLayout.CENTER);

        tuitionTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && tuitionTable.getSelectedRow() >= 0) {
                    int row = tuitionTable.getSelectedRow();
                    selectedTuitionId = (Integer) tuitionModel.getValueAt(row, 0);
                    updateSelectedTuitionDetails();
                }
            }
        });

        btnPayFull.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedTuitionId > 0) {
                    long rem = service.getRemainingDebt(selectedTuitionId);
                    txtPayAmount.setText(String.valueOf(rem));
                }
            }
        });

        btnPay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedTuitionId < 0) {
                    JOptionPane.showMessageDialog(TuitionPaymentPanel.this, "Vui lòng chọn một khoản học phí trong bảng để thanh toán!");
                    return;
                }
                try {
                    long amount = Long.parseLong(FormatUtil.parseCleanDigits(txtPayAmount.getText()));
                    Payment p = service.recordPayment(
                            selectedTuitionId,
                            amount,
                            (String) cboMethod.getSelectedItem(),
                            txtNote.getText()
                    );
                    long newDebt = service.getRemainingDebt(selectedTuitionId);
                    refreshData();
                    if (onDataChanged != null) onDataChanged.run();
                    JOptionPane.showMessageDialog(TuitionPaymentPanel.this,
                            "Ghi nhận giao dịch thanh toán thành công!\n"
                                    + "- Mã phiếu thu: " + p.getPaymentCode() + "\n"
                                    + "- Số tiền vừa thu: " + FormatUtil.formatVnd(p.getAmount()) + "\n"
                                    + "- Số tiền còn nợ mới (BR-19): " + FormatUtil.formatVnd(newDebt),
                            "Thành công (TC-06 / UC-05)", JOptionPane.INFORMATION_MESSAGE);
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(TuitionPaymentPanel.this, "Số tiền thanh toán phải là chữ số hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                } catch (ValidationException ex) {
                    JOptionPane.showMessageDialog(TuitionPaymentPanel.this,
                            "[" + ex.getRuleCode() + "] " + ex.getMessage(),
                            "Từ chối giao dịch thanh toán (TC-07)", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnSearch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshData();
            }
        });

        refreshData();
        if (tuitionModel.getRowCount() > 0) {
            // Chọn khoản học phí số 5 (4.000.000 VNĐ -> đã đóng 1.5tr + 2tr -> còn nợ 500.000 VNĐ) để minh họa đẹp nhất!
            int targetRow = Math.min(4, tuitionModel.getRowCount() - 1);
            tuitionTable.setRowSelectionInterval(targetRow, targetRow);
        }
    }

    private JPanel createInfoBox(String title, JLabel valLabel, Color color) {
        JPanel p = new JPanel(new BorderLayout(2, 2));
        p.setBackground(new Color(248, 250, 252));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        JLabel t = new JLabel(title);
        t.setFont(UITheme.FONT_SMALL);
        t.setForeground(UITheme.TEXT_MUTED);
        valLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        valLabel.setForeground(color);
        p.add(t, BorderLayout.NORTH);
        p.add(valLabel, BorderLayout.CENTER);
        return p;
    }

    private void updateSelectedTuitionDetails() {
        TuitionFee tf = repo.findTuitionById(selectedTuitionId);
        if (tf == null) return;
        Enrollment e = repo.findEnrollmentById(tf.getEnrollmentId());
        Student s = e != null ? repo.findStudentById(e.getStudentId()) : null;
        ClassRoom c = e != null ? repo.findClassById(e.getClassId()) : null;
        long due = tf.getAmountDue();
        long paid = service.getTotalPaidForTuition(tf.getTuitionId());
        long debt = service.getRemainingDebt(tf.getTuitionId());

        lblSelectedInfo.setText((e != null ? e.getEnrollmentCode() : "") + " - "
                + (s != null ? s.getFullName() : "") + " (" + (c != null ? c.getClassCode() : "") + ")");
        lblAmountDue.setText(FormatUtil.formatVnd(due));
        lblTotalPaid.setText(FormatUtil.formatVnd(paid));
        lblRemainingDebt.setText(FormatUtil.formatVnd(debt));
        if (debt > 0) {
            txtPayAmount.setText(String.valueOf(debt));
        }

        paymentHistoryModel.setRowCount(0);
        List<Payment> list = service.getPaymentsByTuitionId(selectedTuitionId);
        for (Payment p : list) {
            paymentHistoryModel.addRow(new Object[]{
                    p.getPaymentCode(),
                    "HP#" + p.getTuitionId(),
                    FormatUtil.formatVnd(p.getAmount()),
                    p.getPaymentDate(),
                    p.getMethod(),
                    p.getReceivedBy(),
                    p.getNote()
            });
        }
    }

    public void refreshData() {
        String kw = txtFilter.getText() == null ? "" : txtFilter.getText().trim().toLowerCase();
        tuitionModel.setRowCount(0);
        for (TuitionFee tf : repo.getTuitionFees()) {
            Enrollment e = repo.findEnrollmentById(tf.getEnrollmentId());
            Student s = e != null ? repo.findStudentById(e.getStudentId()) : null;
            ClassRoom c = e != null ? repo.findClassById(e.getClassId()) : null;
            Course course = c != null ? repo.findCourseById(c.getCourseId()) : null;

            String studentName = s != null ? s.getFullName() : "";
            String studentCode = s != null ? s.getStudentCode() : "";
            String classCode = c != null ? c.getClassCode() : "";
            if (!kw.isEmpty() && !studentName.toLowerCase().contains(kw)
                    && !studentCode.toLowerCase().contains(kw)
                    && !classCode.toLowerCase().contains(kw)) {
                continue;
            }

            long due = tf.getAmountDue();
            long paid = service.getTotalPaidForTuition(tf.getTuitionId());
            long debt = service.getRemainingDebt(tf.getTuitionId());

            tuitionModel.addRow(new Object[]{
                    tf.getTuitionId(),
                    e != null ? e.getEnrollmentCode() : "",
                    studentCode,
                    studentName,
                    classCode,
                    course != null ? course.getCourseName() : "",
                    FormatUtil.formatVnd(due),
                    FormatUtil.formatVnd(paid),
                    FormatUtil.formatVnd(debt),
                    tf.getStatus()
            });
        }
        if (selectedTuitionId > 0) {
            updateSelectedTuitionDetails();
        }
    }
}
