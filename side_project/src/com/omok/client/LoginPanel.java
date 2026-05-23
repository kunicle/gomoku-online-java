package com.omok.client;

import com.omok.db.UserDAO;
import com.omok.vo.UserVO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

/**
 * Login and registration screen.
 * Displayed as the first screen when the application launches.
 */
public class LoginPanel extends JPanel {

    private final MainFrame mainFrame;

    // Login fields
    private final JTextField loginUsernameField;
    private final JPasswordField loginPasswordField;

    // Registration fields
    private final JTextField registerUsernameField;
    private final JPasswordField registerPasswordField;
    private final JTextField registerNicknameField;

    public LoginPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(new Color(240, 240, 245));

        loginUsernameField    = new JTextField();
        loginPasswordField    = new JPasswordField();
        registerUsernameField = new JTextField();
        registerPasswordField = new JPasswordField();
        registerNicknameField = new JTextField();

        add(buildBanner(), BorderLayout.NORTH);
        add(buildFormPanel(), BorderLayout.CENTER);
    }

    // ── Top banner ──
    private JPanel buildBanner() {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(new Color(50, 50, 80));
        banner.setBorder(new EmptyBorder(24, 0, 24, 0));

        JLabel title = new JLabel("五目並べオンライン", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 36));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("OMOK ONLINE", SwingConstants.CENTER);
        subtitle.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitle.setForeground(new Color(180, 180, 210));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        textPanel.setOpaque(false);
        textPanel.add(title);
        textPanel.add(subtitle);

        banner.add(textPanel, BorderLayout.CENTER);
        return banner;
    }

    // ── Form panel (login + register side by side) ──
    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setBackground(new Color(240, 240, 245));
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));

        panel.add(buildLoginPanel());
        panel.add(buildRegisterPanel());

        return panel;
    }

    // ── Login panel ──
    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 210), 1),
                new EmptyBorder(24, 24, 24, 24)
        ));

        JLabel header = new JLabel("ログイン");
        header.setFont(new Font("Arial", Font.BOLD, 18));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(header);
        panel.add(Box.createVerticalStrut(20));
        panel.add(buildFieldGroup("ID", loginUsernameField, null));
        panel.add(Box.createVerticalStrut(12));
        panel.add(buildFieldGroup("パスワード", loginPasswordField, null));
        panel.add(Box.createVerticalStrut(20));

        JButton loginBtn = buildButton("ログイン", new Color(50, 50, 180));
        loginBtn.addActionListener(e -> onLogin());
        panel.add(loginBtn);

        return panel;
    }

    // ── Registration panel ──
    private JPanel buildRegisterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 210), 1),
                new EmptyBorder(24, 24, 24, 24)
        ));

        JLabel header = new JLabel("新規登録");
        header.setFont(new Font("Arial", Font.BOLD, 18));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(header);
        panel.add(Box.createVerticalStrut(20));
        panel.add(buildFieldGroup("ID", registerUsernameField, "4〜20文字、英数字のみ"));
        panel.add(Box.createVerticalStrut(12));
        panel.add(buildFieldGroup("パスワード", registerPasswordField, "4文字以上"));
        panel.add(Box.createVerticalStrut(12));
        panel.add(buildFieldGroup("ニックネーム", registerNicknameField, "2〜10文字、他のユーザーと重複不可"));
        panel.add(Box.createVerticalStrut(20));

        JButton registerBtn = buildButton("登録", new Color(40, 140, 80));
        registerBtn.addActionListener(e -> onRegister());
        panel.add(registerBtn);

        return panel;
    }

    // Label + input field + hint group
    private JPanel buildFieldGroup(String label, JComponent field, String hint) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setOpaque(false);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.PLAIN, 13));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setFont(new Font("Arial", Font.PLAIN, 13));

        group.add(lbl);
        group.add(Box.createVerticalStrut(4));
        group.add(field);

        if (hint != null) {
            JLabel hintLabel = new JLabel(hint);
            hintLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            hintLabel.setForeground(Color.GRAY);
            hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            group.add(Box.createVerticalStrut(2));
            group.add(hintLabel);
        }

        return group;
    }

    // Shared button style
    private JButton buildButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Login handler ──
    private void onLogin() {
        String username = loginUsernameField.getText().trim();
        String password = new String(loginPasswordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("IDとパスワードを入力してください。");
            return;
        }

        try {
            UserDAO dao = new UserDAO();
            UserVO user = dao.login(username, password);

            if (user != null) {
                mainFrame.onLoginSuccess(user);
            } else {
                showError("IDまたはパスワードが正しくありません。");
            }
        } catch (SQLException e) {
            showError("DBエラー: " + e.getMessage());
        }
    }

    // ── Registration handler ──
    private void onRegister() {
        String username = registerUsernameField.getText().trim();
        String password = new String(registerPasswordField.getPassword());
        String nickname = registerNicknameField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || nickname.isEmpty()) {
            showError("すべての項目を入力してください。");
            return;
        }
        if (username.length() < 4 || username.length() > 20) {
            showError("IDは4〜20文字で入力してください。");
            return;
        }
        if (password.length() < 4) {
            showError("パスワードは4文字以上で入力してください。");
            return;
        }
        if (nickname.length() < 2 || nickname.length() > 10) {
            showError("ニックネームは2〜10文字で入力してください。");
            return;
        }

        try {
            UserDAO dao = new UserDAO();
            if (dao.existsByUsername(username)) {
                showError("すでに使用されているIDです。");
                return;
            }
            if (dao.existsByNickname(nickname)) {
                showError("すでに使用されているニックネームです。");
                return;
            }
            dao.insert(username, password, nickname);
            JOptionPane.showMessageDialog(this, "登録が完了しました！");
            registerUsernameField.setText("");
            registerPasswordField.setText("");
            registerNicknameField.setText("");

        } catch (SQLException e) {
            showError("DBエラー: " + e.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "エラー", JOptionPane.ERROR_MESSAGE);
    }
}