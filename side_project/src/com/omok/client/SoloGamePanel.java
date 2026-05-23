package com.omok.client;

import com.omok.db.GameDAO;
import com.omok.db.MoveDAO;
import com.omok.db.UserDAO;
import com.omok.vo.UserVO;

import javax.swing.*;
import java.awt.*;

/**
 * Solo play screen for battling the AI bot.
 * Runs entirely on the client side - no socket connection needed.
 * Communicates with the Python Flask AI server via HTTP.
 */
public class SoloGamePanel extends JPanel {

    private static final int BOARD_SIZE = 19;

    private final MainFrame mainFrame;
    private UserVO currentUser;
    private BotPlayer botPlayer;

    // Board state: 0=empty, 1=black, 2=white
    private final int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
    private int moveCount = 0;
    private int myColor;    // Player's stone color
    private int botColor;   // AI's stone color
    private boolean gameOver = false;
    private boolean myTurn = false;
    private long turnStartTime;
    private long gameId = -1;

    // UI components
    private BoardPanel boardPanel;
    private JLabel turnLabel;
    private JLabel timerLabel;
    private JLabel myInfoLabel;
    private JLabel botInfoLabel;
    private JTextArea logArea;
    private JButton resignBtn;
    private JButton lobbyBtn;
    private Timer countdownTimer;
    private int timeLeft = 30;

    public SoloGamePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 0));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        boardPanel = new BoardPanel();
        boardPanel.setClickListener((row, col) -> onPlayerMove(row, col));

        JPanel boardWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        boardWrapper.setBackground(new Color(240, 230, 210));
        boardWrapper.add(boardPanel);

        add(boardWrapper, BorderLayout.CENTER);
        add(buildSidePanel(), BorderLayout.EAST);
    }

    // ── Right side panel ──
    private JPanel buildSidePanel() {
        JPanel side = new JPanel(new BorderLayout(0, 10));
        side.setPreferredSize(new Dimension(200, 0));
        side.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        side.add(buildInfoPanel(), BorderLayout.NORTH);
        side.add(buildLogPanel(), BorderLayout.CENTER);
        return side;
    }

    private JPanel buildInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 1, 0, 6));
        panel.setBorder(BorderFactory.createTitledBorder("ゲーム情報"));

        myInfoLabel  = new JLabel("自分: -");
        botInfoLabel = new JLabel("AI: -");
        turnLabel    = new JLabel("待機中...");
        turnLabel.setFont(new Font(turnLabel.getFont().getName(), Font.BOLD, 13));
        timerLabel = new JLabel("⏱ --秒");
        timerLabel.setForeground(new Color(180, 60, 60));

        resignBtn = new JButton("投了");
        resignBtn.setForeground(Color.RED);
        resignBtn.addActionListener(e -> onResign());

        lobbyBtn = new JButton("ロビーに戻る");
        lobbyBtn.setEnabled(false);
        lobbyBtn.addActionListener(e -> {
            stopTimer();
            mainFrame.showLobby();
        });

        panel.add(myInfoLabel);
        panel.add(botInfoLabel);
        panel.add(turnLabel);
        panel.add(timerLabel);
        panel.add(resignBtn);
        panel.add(lobbyBtn);

        return panel;
    }

    private JPanel buildLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("ゲームログ"));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return panel;
    }

    // ── Game initialization ──
    public void startGame(UserVO user, int playerColor) {
        this.currentUser = user;
        this.myColor  = playerColor;
        this.botColor = (playerColor == 1) ? 2 : 1;
        this.botPlayer = new BotPlayer(botColor);
        this.gameOver = false;
        this.moveCount = 0;

        // Reset board
        for (int[] row : board) java.util.Arrays.fill(row, 0);
        boardPanel.reset();
        logArea.setText("");
        resignBtn.setEnabled(true);
        lobbyBtn.setEnabled(false);

        String myColorStr  = (myColor  == 1) ? "黒石 ●" : "白石 ○";
        String botColorStr = (botColor == 1) ? "黒石 ●" : "白石 ○";
        myInfoLabel.setText("自分: " + user.getNickname() + " (" + myColorStr + ")");
        botInfoLabel.setText("AI: Bot (" + botColorStr + ")");

        // Save game start to DB
        try {
            GameDAO gameDAO = new GameDAO();
            this.gameId = gameDAO.insertSolo(user.getUserId());
        } catch (Exception e) {
            System.out.println("Failed to save solo game to DB: " + e.getMessage());
        }

        addLog(myColor == 1 ? "ゲーム開始！黒石で先手です。" : "ゲーム開始！白石です。");

        // Black stone goes first
        if (myColor == 1) {
            setMyTurn(true);
        } else {
            setMyTurn(false);
            requestBotMove();
        }
    }

    // ── Player move handler ──
    private void onPlayerMove(int row, int col) {
        if (gameOver || !myTurn) return;
        if (board[row][col] != 0) return;

        stopTimer();
        int thinkTimeMs = (int)(System.currentTimeMillis() - turnStartTime);

        placeStone(row, col, myColor, thinkTimeMs);
        addLog(String.format("%d手: 自分 (%d,%d) %.1f秒",
                moveCount, row, col, thinkTimeMs / 1000.0));

        if (checkWin(row, col, myColor)) { endGame(true); return; }
        if (moveCount == BOARD_SIZE * BOARD_SIZE) { endGameDraw(); return; }

        // AI's turn
        setMyTurn(false);
        Timer delay = new Timer(300, e -> requestBotMove());
        delay.setRepeats(false);
        delay.start();
    }

    // ── Request AI move from Flask server ──
    private void requestBotMove() {
        if (gameOver) return;
        turnLabel.setText("AI 思考中...");
        turnLabel.setForeground(new Color(100, 100, 200));

        // Run AI request on separate thread to avoid blocking UI
        new Thread(() -> {
            long botTurnStart = System.currentTimeMillis();
            int[] move = botPlayer.requestMove(board);
            int thinkTimeMs = (int)(System.currentTimeMillis() - botTurnStart);

            // Fallback to random move if AI server fails
            final int[] finalMove = (move != null) ? move : findRandomEmpty();

            SwingUtilities.invokeLater(() -> {
                if (gameOver) return;
                if (finalMove == null) { addLog("着手可能な位置がありません。"); return; }
                if (move == null) addLog("AIサーバーエラー - ランダム着手");

                placeStone(finalMove[0], finalMove[1], botColor, thinkTimeMs);
                addLog(String.format("%d手: AI (%d,%d) %.1f秒",
                        moveCount, finalMove[0], finalMove[1], thinkTimeMs / 1000.0));

                if (checkWin(finalMove[0], finalMove[1], botColor)) { endGame(false); return; }
                if (moveCount == BOARD_SIZE * BOARD_SIZE) { endGameDraw(); return; }
                setMyTurn(true);
            });
        }).start();
    }

    // ── Stone placement ──
    private void placeStone(int row, int col, int color, int thinkTimeMs) {
        board[row][col] = color;
        moveCount++;
        boardPanel.placeStone(row, col, color);

        // Save move to DB
        try {
            if (gameId > 0) {
                MoveDAO moveDAO = new MoveDAO();
                String colorStr = (color == 1) ? "BLACK" : "WHITE";
                if (color == myColor) {
                    moveDAO.insert(gameId, currentUser.getUserId(),
                            moveCount, row, col, colorStr, thinkTimeMs);
                } else {
                    moveDAO.insertAI(gameId, moveCount, row, col, colorStr, thinkTimeMs);
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to save move to DB: " + e.getMessage());
        }
    }

    // ── Win detection (same logic as server-side GameSession) ──
    private boolean checkWin(int row, int col, int color) {
        int[][] directions = {{0,1},{1,0},{1,1},{1,-1}};
        for (int[] dir : directions) {
            int count = 1;
            count += countStones(row, col,  dir[0],  dir[1], color);
            count += countStones(row, col, -dir[0], -dir[1], color);
            if (count >= 5) return true;
        }
        return false;
    }

    private int countStones(int row, int col, int dr, int dc, int color) {
        int count = 0;
        int r = row + dr, c = col + dc;
        while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE
                && board[r][c] == color) {
            count++; r += dr; c += dc;
        }
        return count;
    }

    // ── Game end handlers ──
    private void endGame(boolean playerWin) {
        gameOver = true;
        stopTimer();
        boardPanel.setMyTurn(false);
        resignBtn.setEnabled(false);
        lobbyBtn.setEnabled(true);

        String message = playerWin ? "🎉 勝利しました！" : "😢 AIに敗北しました。";
        turnLabel.setText(message);
        turnLabel.setForeground(playerWin ? new Color(0, 120, 0) : new Color(180, 0, 0));
        addLog(message);

        // Save result to DB (win record not affected for solo games)
        try {
            if (gameId > 0) {
                long winnerId = playerWin ? currentUser.getUserId() : 0;
                String result = (myColor == 1)
                        ? (playerWin ? "BLACK_WIN" : "WHITE_WIN")
                        : (playerWin ? "WHITE_WIN" : "BLACK_WIN");
                String winPos = buildWinPositions(board, playerWin ? myColor : botColor);

                GameDAO gameDAO = new GameDAO();
                gameDAO.update(gameId, winnerId, result, moveCount, winPos);
            }
        } catch (Exception e) {
            System.out.println("Failed to save solo game result: " + e.getMessage());
        }

        Timer delay = new Timer(500, e ->
                JOptionPane.showMessageDialog(this, message, "ゲーム終了",
                        JOptionPane.INFORMATION_MESSAGE));
        delay.setRepeats(false);
        delay.start();
    }

    private void endGameDraw() {
        gameOver = true;
        stopTimer();
        boardPanel.setMyTurn(false);
        resignBtn.setEnabled(false);
        lobbyBtn.setEnabled(true);
        turnLabel.setText("引き分け！");
        addLog("引き分け！");

        try {
            if (gameId > 0) {
                GameDAO gameDAO = new GameDAO();
                gameDAO.update(gameId, 0, "DRAW", moveCount, "");
            }
        } catch (Exception e) {
            System.out.println("Failed to save draw result: " + e.getMessage());
        }
    }

    private void onResign() {
        int choice = JOptionPane.showConfirmDialog(this,
                "本当に投了しますか？", "投了", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) { endGame(false); }
    }

    // ── Timer ──
    private void setMyTurn(boolean isMyTurn) {
        myTurn = isMyTurn;
        boardPanel.setMyTurn(isMyTurn);
        if (isMyTurn) {
            turnLabel.setText("● 自分の番");
            turnLabel.setForeground(new Color(0, 120, 0));
            turnStartTime = System.currentTimeMillis();
            startTimer();
        } else {
            stopTimer();
        }
    }

    private void startTimer() {
        stopTimer();
        timeLeft = 30;
        timerLabel.setText("⏱ " + timeLeft + "秒");
        countdownTimer = new Timer(1000, e -> {
            timeLeft--;
            timerLabel.setText("⏱ " + timeLeft + "秒");
            timerLabel.setForeground(timeLeft <= 10
                    ? new Color(200, 0, 0) : new Color(180, 60, 60));
            if (timeLeft <= 0) { stopTimer(); addLog("時間切れ！敗北"); endGame(false); }
        });
        countdownTimer.start();
    }

    private void stopTimer() {
        if (countdownTimer != null) { countdownTimer.stop(); countdownTimer = null; }
        timerLabel.setText("⏱ --秒");
    }

    private void addLog(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private int[] findRandomEmpty() {
        java.util.List<int[]> empty = new java.util.ArrayList<>();
        for (int r = 0; r < BOARD_SIZE; r++)
            for (int c = 0; c < BOARD_SIZE; c++)
                if (board[r][c] == 0) empty.add(new int[]{r, c});
        return empty.isEmpty() ? null : empty.get((int)(Math.random() * empty.size()));
    }

    /**
     * Finds and returns the winning 5 stone positions as a formatted string.
     * Format: "r1,c1;r2,c2;r3,c3;r4,c4;r5,c5"
     * Used for replay highlight display.
     */
    private String buildWinPositions(int[][] board, int color) {
        int[][] directions = {{0,1},{1,0},{1,1},{1,-1}};
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (board[row][col] != color) continue;
                for (int[] dir : directions) {
                    java.util.List<int[]> stones = new java.util.ArrayList<>();
                    stones.add(new int[]{row, col});
                    for (int d = 1; d <= 4; d++) {
                        int r = row + dir[0]*d, c = col + dir[1]*d;
                        if (r<0||r>=BOARD_SIZE||c<0||c>=BOARD_SIZE||board[r][c]!=color) break;
                        stones.add(new int[]{r, c});
                    }
                    for (int d = 1; d <= 4; d++) {
                        int r = row - dir[0]*d, c = col - dir[1]*d;
                        if (r<0||r>=BOARD_SIZE||c<0||c>=BOARD_SIZE||board[r][c]!=color) break;
                        stones.add(new int[]{r, c});
                    }
                    if (stones.size() >= 5) {
                        StringBuilder sb = new StringBuilder();
                        for (int[] s : stones) {
                            if (sb.length() > 0) sb.append(";");
                            sb.append(s[0]).append(",").append(s[1]);
                        }
                        return sb.toString();
                    }
                }
            }
        }
        return "";
    }
}