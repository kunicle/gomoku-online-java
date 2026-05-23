package com.omok.client;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for selecting stone color before starting a solo game against AI.
 */
public class SoloStartDialog extends JDialog {

    private int selectedColor = -1;  // 1=black, 2=white, -1=cancelled

    public SoloStartDialog(JFrame parent) {
        super(parent, "AI対戦", true);
        setLayout(new BorderLayout(10, 10));
        setSize(340, 220);
        setLocationRelativeTo(parent);
        setResizable(false);

        JLabel label = new JLabel("石の色を選んでください。", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 14));

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JButton blackBtn = new JButton("黒石 ●（先手）");
        JButton whiteBtn = new JButton("白石 ○（後手）");

        blackBtn.setPreferredSize(new Dimension(120, 40));
        whiteBtn.setPreferredSize(new Dimension(120, 40));

        blackBtn.addActionListener(e -> { selectedColor = 1; dispose(); });
        whiteBtn.addActionListener(e -> { selectedColor = 2; dispose(); });

        btnPanel.add(blackBtn);
        btnPanel.add(whiteBtn);

        add(label, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    // Returns selected color (-1 if cancelled)
    public int getSelectedColor() { return selectedColor; }
}