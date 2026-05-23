package com.omok.client;

import com.omok.db.StatsDAO;
import com.omok.db.UserDAO;
import com.omok.vo.GameStatsVO;
import com.omok.vo.UserVO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Statistics screen showing win rate, average think time, and overall ranking.
 * Visualizes win rate with a pie chart drawn via Graphics2D.
 */
public class StatsPanel extends JPanel {

    private final MainFrame mainFrame;

    // Stats labels
    private JLabel totalGamesLabel;
    private JLabel winRateLabel;
    private JLabel avgThinkTimeLabel;
    private JLabel avgTotalMovesLabel;
    private JLabel winningMoveLabel;

    // Ranking table
    private DefaultTableModel rankingTableModel;

    // Win rate value for pie chart rendering
    private double currentWinRate = 0;

    public StatsPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildRankingPanel(), BorderLayout.CENTER);
        add(buildBackButton(), BorderLayout.SOUTH);
    }

    // ── Top: stats + chart ──
    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.add(buildMyStatsPanel());
        panel.add(buildChartPanel());
        return panel;
    }

    // Player stats panel
    private JPanel buildMyStatsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("自分の統計"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(6, 12, 6, 12);

        totalGamesLabel   = new JLabel("総ゲーム数: -");
        winRateLabel      = new JLabel("勝率: -");
        avgThinkTimeLabel = new JLabel("平均着手時間: -");
        avgTotalMovesLabel = new JLabel("平均ゲーム手数: -");
        winningMoveLabel  = new JLabel("平均勝利手数: -");

        Font statFont = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
        int gridy = 0;
        for (JLabel lbl : new JLabel[]{totalGamesLabel, winRateLabel,
                avgThinkTimeLabel, avgTotalMovesLabel, winningMoveLabel}) {
            lbl.setFont(statFont);
            gbc.gridy = gridy++;
            panel.add(lbl, gbc);
        }

        return panel;
    }

    // Win rate pie chart panel
    private JPanel buildChartPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();
                int cx = w / 2, cy = h / 2;
                int r = Math.min(w, h) / 2 - 20;

                // Background circle (gray = losses)
                g2.setColor(new Color(220, 220, 220));
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);

                // Win rate arc (green)
                g2.setColor(new Color(76, 153, 76));
                g2.fillArc(cx - r, cy - r, r * 2, r * 2,
                        90, -(int)(currentWinRate * 360));

                // Center text
                g2.setColor(Color.BLACK);
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
                String text = String.format("%.1f%%", currentWinRate * 100);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text,
                        cx - fm.stringWidth(text) / 2, cy + fm.getAscent() / 2);
            }
        };
        panel.setBorder(BorderFactory.createTitledBorder("勝率"));
        panel.setPreferredSize(new Dimension(180, 180));
        return panel;
    }

    // ── Center: Ranking table ──
    private JPanel buildRankingPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createTitledBorder("全体ランキング"));

        String[] columns = {"順位", "ニックネーム", "勝", "負", "引分", "総ゲーム", "勝率"};
        rankingTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = new JTable(rankingTableModel);
        table.setRowHeight(28);
        table.getTableHeader().setReorderingAllowed(false);

        int[] widths = {40, 120, 40, 40, 40, 60, 60};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // ── Bottom: Back button ──
    private JPanel buildBackButton() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton backBtn = new JButton("ロビーに戻る");
        backBtn.addActionListener(e -> mainFrame.showLobby());
        panel.add(backBtn);
        return panel;
    }

    // Load stats on screen entry
    public void refresh(UserVO user) {
        loadMyStats(user.getUserId());
        loadRanking();
    }

    // Load player stats from DB
    private void loadMyStats(long userId) {
        try {
            StatsDAO dao = new StatsDAO();
            GameStatsVO stats = dao.getStatsByUserId(userId);
            currentWinRate = stats.getWinRate();

            totalGamesLabel.setText("総ゲーム数: " + stats.getTotalGames() + "局");
            winRateLabel.setText(String.format("勝率: %.1f%%", stats.getWinRate() * 100));
            avgThinkTimeLabel.setText(String.format("平均着手時間: %.1f秒",
                    stats.getAvgThinkTimeMs() / 1000.0));
            avgTotalMovesLabel.setText(String.format("平均ゲーム手数: %.0f手",
                    stats.getAvgTotalMoves()));
            winningMoveLabel.setText(String.format("平均勝利手数: %.0f手",
                    stats.getWinningMoveAvg()));

            repaint();  // Redraw pie chart

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "統計読み込み失敗: " + e.getMessage());
        }
    }

    // Load ranking from DB
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
                        total,
                        winRate
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "ランキング読み込み失敗: " + e.getMessage());
        }
    }
}