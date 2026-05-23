package com.omok.client;

import com.omok.db.UserDAO;
import com.omok.vo.UserVO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Lobby screen displayed after login.
 * Shows player profile, ranking table, and matchmaking controls.
 */
public class LobbyPanel extends JPanel {

    private final MainFrame mainFrame;

    // Matchmaking state
    private boolean isWaiting = false;

    // UI components
    private JLabel nicknameLabel;
    private JLabel recordLabel;
    private JButton matchButton;
    private JLabel matchStatusLabel;
    private DefaultTableModel rankingTableModel;

    public LobbyPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(buildProfilePanel(), BorderLayout.WEST);
        add(buildRankingPanel(), BorderLayout.CENTER);
        add(buildMatchPanel(), BorderLayout.SOUTH);
    }

    // ── Left: Player profile panel ──
    private JPanel buildProfilePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("プロフィール"));
        panel.setPreferredSize(new Dimension(200, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(6, 10, 6, 10);

        nicknameLabel = new JLabel("ニックネーム: -");
        nicknameLabel.setFont(new Font(nicknameLabel.getFont().getName(), Font.BOLD, 14));
        gbc.gridy = 0;
        panel.add(nicknameLabel, gbc);

        recordLabel = new JLabel("戦績: -");
        gbc.gridy = 1;
        panel.add(recordLabel, gbc);

        return panel;
    }

    // ── Center: Ranking table ──
    private JPanel buildRankingPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createTitledBorder("ランキング"));

        String[] columns = {"順位", "ニックネーム", "勝", "負", "引分", "勝率"};
        rankingTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable rankingTable = new JTable(rankingTableModel);
        rankingTable.setRowHeight(28);
        rankingTable.getTableHeader().setReorderingAllowed(false);

        int[] widths = {50, 140, 50, 50, 50, 70};
        for (int i = 0; i < widths.length; i++) {
            rankingTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        panel.add(new JScrollPane(rankingTable), BorderLayout.CENTER);

        JButton refreshBtn = new JButton("ランキング更新");
        refreshBtn.addActionListener(e -> loadRanking());
        panel.add(refreshBtn, BorderLayout.SOUTH);

        return panel;
    }

    // ── Bottom: Matchmaking panel ──
    private JPanel buildMatchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panel.setBorder(BorderFactory.createTitledBorder("マッチング"));

        matchButton = new JButton("マッチング開始");
        matchButton.setPreferredSize(new Dimension(140, 40));
        matchButton.addActionListener(e -> onMatchButtonClick());

        matchStatusLabel = new JLabel("マッチングボタンを押して対戦相手を探してください。");
        matchStatusLabel.setForeground(Color.GRAY);

        JButton statsBtn = new JButton("統計を見る");
        statsBtn.addActionListener(e -> mainFrame.showStats());

        JButton replayBtn = new JButton("リプレイ");
        replayBtn.addActionListener(e -> mainFrame.showReplay());

        JButton soloBtn = new JButton("AI対戦");
        soloBtn.addActionListener(e -> mainFrame.showSolo());

        panel.add(matchButton);
        panel.add(matchStatusLabel);
        panel.add(statsBtn);
        panel.add(replayBtn);
        panel.add(soloBtn);

        return panel;
    }

    // ── Matchmaking button click ──
    private void onMatchButtonClick() {
        if (!isWaiting) {
            mainFrame.sendToServer(com.omok.common.MessageProtocol.MATCH_REQUEST);
            isWaiting = true;
            matchButton.setText("キャンセル");
            matchStatusLabel.setText("対戦相手を探しています...");
            matchStatusLabel.setForeground(new Color(0, 120, 0));
        } else {
            mainFrame.sendToServer(com.omok.common.MessageProtocol.MATCH_CANCEL);
            isWaiting = false;
            matchButton.setText("マッチング開始");
            matchStatusLabel.setText("マッチングをキャンセルしました。");
            matchStatusLabel.setForeground(Color.GRAY);
        }
    }

    // Called by MainFrame when match is found
    public void onMatchFound() {
        isWaiting = false;
        matchButton.setText("マッチング開始");
        matchStatusLabel.setText("マッチング成立！ゲームを開始します...");
        matchStatusLabel.setForeground(new Color(0, 80, 200));
    }

    // Reset match status on lobby entry
    private void resetMatchStatus() {
        isWaiting = false;
        matchButton.setText("マッチング開始");
        matchStatusLabel.setText("マッチングボタンを押して対戦相手を探してください。");
        matchStatusLabel.setForeground(Color.GRAY);
    }

    // Load ranking data from DB
    private void loadRanking() {
        try {
            UserDAO dao = new UserDAO();
            List<UserVO> ranking = dao.getRanking(50);

            rankingTableModel.setRowCount(0);
            for (int i = 0; i < ranking.size(); i++) {
                UserVO u = ranking.get(i);
                int total = u.getWinCount() + u.getLoseCount() + u.getDrawCount();
                String winRate = total == 0 ? "0%" :
                        String.format("%.1f%%", (double) u.getWinCount() / total * 100);
                rankingTableModel.addRow(new Object[]{
                        i + 1,
                        u.getNickname(),
                        u.getWinCount(),
                        u.getLoseCount(),
                        u.getDrawCount(),
                        winRate
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "ランキング読み込み失敗: " + e.getMessage());
        }
    }

    // Refresh profile and ranking on lobby entry
    public void refresh(UserVO user) {
        nicknameLabel.setText("ニックネーム: " + user.getNickname());
        recordLabel.setText(String.format("%dW / %dL / %dD",
                user.getWinCount(), user.getLoseCount(), user.getDrawCount()));
        resetMatchStatus();
        loadRanking();
    }
}