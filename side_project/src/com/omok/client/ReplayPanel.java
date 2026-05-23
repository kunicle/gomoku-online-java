package com.omok.client;

import com.omok.db.GameDAO;
import com.omok.db.MoveDAO;
import com.omok.db.UserDAO;
import com.omok.vo.GameVO;
import com.omok.vo.MoveVO;
import com.omok.vo.UserVO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Replay screen for reviewing past games.
 * Loads move records from DB and plays them back on the board step by step.
 */
public class ReplayPanel extends JPanel {

    private final MainFrame mainFrame;

    // Current replay data
    private List<MoveVO> moves;
    private int currentMoveIndex = 0;
    private int[][] parsedWinStones = null;

    // Auto-play timer
    private Timer autoPlayTimer;

    // UI components
    private BoardPanel boardPanel;
    private JLabel moveInfoLabel;
    private JLabel gameInfoLabel;
    private JButton prevBtn, nextBtn, firstBtn, autoPlayBtn;
    private DefaultTableModel gameListModel;
    private JTable gameListTable;

    public ReplayPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        boardPanel = new BoardPanel();
        boardPanel.setMyTurn(false);  // Disable clicks during replay

        JPanel boardWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        boardWrapper.setBackground(new Color(240, 230, 210));
        boardWrapper.add(boardPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildGameListPanel(), boardWrapper);
        splitPane.setDividerLocation(220);
        splitPane.setDividerSize(4);

        add(splitPane, BorderLayout.CENTER);
        add(buildControlPanel(), BorderLayout.SOUTH);
    }

    // ── Left: Game list ──
    private JPanel buildGameListPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createTitledBorder("ゲーム履歴"));
        panel.setPreferredSize(new Dimension(220, 0));

        String[] columns = {"日付", "相手", "結果", "手数"};
        gameListModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        gameListTable = new JTable(gameListModel);
        gameListTable.setRowHeight(26);
        gameListTable.getTableHeader().setReorderingAllowed(false);
        gameListTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        gameListTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        gameListTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        gameListTable.getColumnModel().getColumn(3).setPreferredWidth(40);

        // Load replay when game is selected
        gameListTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && gameListTable.getSelectedRow() >= 0) {
                loadReplay(gameListTable.getSelectedRow());
            }
        });

        panel.add(new JScrollPane(gameListTable), BorderLayout.CENTER);
        return panel;
    }

    // ── Bottom: Control panel ──
    private JPanel buildControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));

        gameInfoLabel = new JLabel(" ");
        gameInfoLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        gameInfoLabel.setForeground(Color.GRAY);

        moveInfoLabel = new JLabel("ゲームを選択してください。");
        moveInfoLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));

        firstBtn    = new JButton("⏮ 最初");
        prevBtn     = new JButton("◀ 前へ");
        nextBtn     = new JButton("次へ ▶");
        autoPlayBtn = new JButton("▶ 自動再生");

        JButton lobbyBtn = new JButton("ロビーに戻る");
        lobbyBtn.addActionListener(e -> {
            stopAutoPlay();
            mainFrame.showLobby();
        });

        firstBtn.addActionListener(e -> goFirst());
        prevBtn.addActionListener(e -> goPrev());
        nextBtn.addActionListener(e -> goNext());
        autoPlayBtn.addActionListener(e -> toggleAutoPlay());

        setControlsEnabled(false);

        btnRow.add(firstBtn);
        btnRow.add(prevBtn);
        btnRow.add(nextBtn);
        btnRow.add(autoPlayBtn);
        btnRow.add(Box.createHorizontalStrut(20));
        btnRow.add(lobbyBtn);

        panel.add(gameInfoLabel, BorderLayout.NORTH);
        panel.add(moveInfoLabel, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);

        return panel;
    }

    // Refresh game list on screen entry
    public void refresh(UserVO user) {
        stopAutoPlay();
        boardPanel.reset();
        moves = null;
        currentMoveIndex = 0;
        moveInfoLabel.setText("ゲームを選択してください。");
        gameInfoLabel.setText(" ");
        setControlsEnabled(false);
        loadGameList(user.getUserId());
    }

    // Load game list from DB
    @SuppressWarnings("unchecked")
    private void loadGameList(long userId) {
        try {
            GameDAO dao = new GameDAO();
            List<GameVO> games = dao.findByUserId(userId);
            UserDAO userDAO = new UserDAO();

            gameListModel.setRowCount(0);
            gameListTable.putClientProperty("games", games);

            for (GameVO g : games) {
                // Determine result text
                String resultText;
                if (g.getResult() == null) {
                    resultText = "未完了";
                } else if (g.getWinnerId() == userId) {
                    resultText = "勝利";
                } else if (g.getResult().equals("DRAW")) {
                    resultText = "引き分け";
                } else {
                    resultText = "敗北";
                }

                String date = g.getStartedAt() != null
                        ? g.getStartedAt().toLocalDate().toString() : "-";

                // Determine opponent nickname
                String opponentNick;
                long opponentId = (g.getPlayer1Id() == userId)
                        ? g.getPlayer2Id() : g.getPlayer1Id();

                if (opponentId <= 0) {
                    opponentNick = "AI Bot";
                } else {
                    try {
                        UserVO opponent = userDAO.findByUserId(opponentId);
                        opponentNick = (opponent != null) ? opponent.getNickname() : "不明";
                    } catch (Exception e) {
                        opponentNick = "不明";
                    }
                }

                gameListModel.addRow(new Object[]{
                        date, opponentNick, resultText, g.getTotalMoves()
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "ゲーム履歴読み込み失敗: " + e.getMessage());
        }
    }

    // Load move records for selected game
    @SuppressWarnings("unchecked")
    private void loadReplay(int tableRow) {
        try {
            List<GameVO> games = (List<GameVO>)
                    gameListTable.getClientProperty("games");
            if (games == null || tableRow >= games.size()) return;

            GameVO game = games.get(tableRow);

            MoveDAO moveDAO = new MoveDAO();
            moves = moveDAO.findByGameId(game.getGameId());

            if (moves.isEmpty()) {
                moveInfoLabel.setText("着手記録がありません。");
                return;
            }

            // Parse win positions before rendering
            parsedWinStones = null;
            boardPanel.highlightWin(null);
            if (game.getWinPositions() != null && !game.getWinPositions().isEmpty()) {
                parseAndHighlight(game.getWinPositions());
            }

            boardPanel.reset();
            currentMoveIndex = 0;
            stopAutoPlay();
            setControlsEnabled(true);

            // Determine resign/forfeit label
            long myUserId = mainFrame.getCurrentUser().getUserId();
            String resignMark = "";
            boolean isResignGame = (game.getWinPositions() == null ||
                    game.getWinPositions().isEmpty()) && game.getWinnerId() > 0;
            if (isResignGame) {
                resignMark = (game.getWinnerId() == myUserId)
                        ? "  |  相手の投了により勝利" : "  |  投了により敗北";
            }

            gameInfoLabel.setText(String.format("game_id: %d  |  全%d手%s",
                    game.getGameId(), moves.size(), resignMark));
            updateMoveInfo();
            renderBoard();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "リプレイ読み込み失敗: " + e.getMessage());
        }
    }

    // Render board up to currentMoveIndex
    private void renderBoard() {
        boardPanel.reset();
        for (int i = 0; i <= currentMoveIndex && i < moves.size(); i++) {
            MoveVO move = moves.get(i);
            int color = move.getStoneColor().equals("BLACK") ? 1 : 2;
            boardPanel.placeStone(move.getRowPos(), move.getColPos(), color);
        }
        // Show win highlight at last move
        if (currentMoveIndex == moves.size() - 1 && parsedWinStones != null) {
            boardPanel.highlightWin(parsedWinStones);
        }
    }

    // ── Playback controls ──

    private void goFirst() {
        if (moves == null) return;
        currentMoveIndex = 0;
        renderBoard();
        updateMoveInfo();
    }

    private void goPrev() {
        if (moves == null || currentMoveIndex <= 0) return;
        currentMoveIndex--;
        renderBoard();
        updateMoveInfo();
    }

    private void goNext() {
        if (moves == null || currentMoveIndex >= moves.size() - 1) return;
        currentMoveIndex++;
        renderBoard();
        updateMoveInfo();
    }

    private void toggleAutoPlay() {
        if (autoPlayTimer != null && autoPlayTimer.isRunning()) {
            stopAutoPlay();
        } else {
            startAutoPlay();
        }
    }

    private void startAutoPlay() {
        if (moves == null) return;
        autoPlayBtn.setText("⏸ 一時停止");
        autoPlayTimer = new Timer(800, e -> {
            if (currentMoveIndex < moves.size() - 1) {
                currentMoveIndex++;
                renderBoard();
                updateMoveInfo();
            } else {
                stopAutoPlay();
            }
        });
        autoPlayTimer.start();
    }

    private void stopAutoPlay() {
        if (autoPlayTimer != null) {
            autoPlayTimer.stop();
            autoPlayTimer = null;
        }
        autoPlayBtn.setText("▶ 自動再生");
    }

    // Update move info label
    private void updateMoveInfo() {
        if (moves == null || moves.isEmpty()) return;
        MoveVO current = moves.get(currentMoveIndex);
        String color = current.getStoneColor().equals("BLACK") ? "黒" : "白";
        moveInfoLabel.setText(String.format("%d / %d手  |  %s石  |  (%d, %d)  |  %.1f秒",
                currentMoveIndex + 1, moves.size(),
                color,
                current.getRowPos(), current.getColPos(),
                current.getThinkTimeMs() / 1000.0));
    }

    // Parse win positions string: "r1,c1;r2,c2;..." → int[][]
    private void parseAndHighlight(String winPositions) {
        try {
            String[] parts = winPositions.split(";");
            parsedWinStones = new int[parts.length][2];
            for (int i = 0; i < parts.length; i++) {
                String[] rc = parts[i].split(",");
                parsedWinStones[i][0] = Integer.parseInt(rc[0]);
                parsedWinStones[i][1] = Integer.parseInt(rc[1]);
            }
        } catch (Exception e) {
            System.out.println("Failed to parse win positions: " + e.getMessage());
            parsedWinStones = null;
        }
    }

    private void setControlsEnabled(boolean enabled) {
        firstBtn.setEnabled(enabled);
        prevBtn.setEnabled(enabled);
        nextBtn.setEnabled(enabled);
        autoPlayBtn.setEnabled(enabled);
    }
}