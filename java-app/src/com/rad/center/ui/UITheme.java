package com.rad.center.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class UITheme {
    public static final Color SIDEBAR_BG = new Color(15, 23, 42);      // #0F172A
    public static final Color SIDEBAR_ACTIVE = new Color(37, 99, 235); // #2563EB
    public static final Color SIDEBAR_HOVER = new Color(30, 41, 59);   // #1E293B
    public static final Color BG_LIGHT = new Color(241, 245, 249);     // #F1F5F9
    public static final Color CARD_BG = Color.WHITE;
    public static final Color PRIMARY = new Color(37, 99, 235);        // #2563EB
    public static final Color SUCCESS = new Color(16, 185, 129);       // #10B981
    public static final Color WARNING = new Color(245, 158, 11);       // #F59E0B
    public static final Color DANGER = new Color(239, 68, 68);         // #EF4444
    public static final Color TEXT_DARK = new Color(15, 23, 42);
    public static final Color TEXT_MUTED = new Color(100, 116, 139);
    public static final Color BORDER_COLOR = new Color(203, 213, 225);

    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 20);
    public static final Font FONT_SUBTITLE = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONT_REGULAR = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 12);

    public static JButton createButton(String text, Color bgColor, Color fgColor) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(bgColor);
        btn.setForeground(fgColor);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }

    public static JTextField createTextField(int columns) {
        JTextField tf = new JTextField(columns);
        tf.setFont(FONT_REGULAR);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(6, 8, 6, 8)
        ));
        return tf;
    }

    public static void styleTable(JTable table) {
        table.setFont(FONT_REGULAR);
        table.setRowHeight(30);
        table.setGridColor(new Color(226, 232, 240));
        table.setSelectionBackground(new Color(219, 234, 254));
        table.setSelectionForeground(TEXT_DARK);
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(new Color(248, 250, 252));
        header.setForeground(TEXT_DARK);
        header.setPreferredSize(new Dimension(header.getWidth(), 36));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
    }

    public static JPanel createCardPanel(String title) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(14, 16, 14, 16)
        ));
        if (title != null && !title.isEmpty()) {
            JLabel lbl = new JLabel(title);
            lbl.setFont(FONT_SUBTITLE);
            lbl.setForeground(TEXT_DARK);
            lbl.setBorder(new EmptyBorder(0, 0, 6, 0));
            card.add(lbl, BorderLayout.NORTH);
        }
        return card;
    }

    public static JPanel createKpiCard(String title, JLabel valueLabel, String subtitle, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, accentColor),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COLOR, 1),
                        new EmptyBorder(12, 16, 12, 16)
                )
        ));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(FONT_BOLD);
        lblTitle.setForeground(TEXT_MUTED);

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valueLabel.setForeground(TEXT_DARK);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(FONT_SMALL);
        lblSub.setForeground(accentColor);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(lblSub, BorderLayout.SOUTH);
        return card;
    }
}

