package com.omok.client;

import com.omok.common.MessageProtocol;
import com.omok.vo.UserVO;

import javax.swing.*;
import java.awt.*;

/**
 * Main game screen combining the board, player info, timer, and chat.
 * Receives server messages via MainFrame and updates the UI accordingly.
 */
public class GamePanel extends JPanel {

    private final MainFrame mainFrame;

    // Game info
    private UserVO myUser;
    private String opponentNickname;
    private int myColor;  // 1=black, 2=white

    // UI components
    private BoardPanel boardPanel;
    private JLabel myInfoLabel;
    private JLabel opponentInfoLabel;
    private JLabel turnLabel;
    private JLabel timerLabel;
    private JTextArea chatArea;
    private JTextField chatInput;
    private JButton resignBtn;
    private JButton lobbyButton;

    // 30-second countdown timer
    private Timer countdownTimer;
    private int timeLeft = 30;

    public GamePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 0));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        boardPanel = new BoardPanel();

        // Stone click → send move to server
        boardPanel.setClickListener((row, col) -> {
            mainFrame.sendToServer(MessageProtocol.build(MessageProtocol.MOVE,
                    String.valueOf(row), String.valueOf(col)));
            boardPanel.setMyTurn(false);  // Block input until server confirms
            stopTimer();
        });

        JPanel boardWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        boardWrapper.setBackground(new Color(240, 230, 210));
        boardWrapper.add(boardPanel);

        add(boardWrapper, BorderLayout.CENTER);
        add(buildSidePanel(), BorderLayout.EAST);
    }

    // ── Right side panel (info + chat) ──
    private JPanel buildSidePanel() {
        JPanel side = new JPanel(new BorderLayout(0, 10));
        side.setPreferredSize(new Dimension(200, 0));
        side.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        side.add(buildInfoPanel(), BorderLayout.NORTH);
        side.add(buildChatPanel(), BorderLayout.CENTER);
        return side;
    }

    // Game info panel (player info + timer + buttons)
    private JPanel buildInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 1, 0, 6));
        panel.setBorder(BorderFactory.createTitledBorder("ゲーム情報"));

        opponentInfoLabel = new JLabel("相手: -");
        myInfoLabel       = new JLabel("自分: -");
        turnLabel         = new JLabel("待機中...");
        turnLabel.setFont(new Font(turnLabel.getFont().getName(), Font.BOLD, 13));
        timerLabel = new JLabel("⏱ --秒");
        timerLabel.setForeground(new Color(180, 60, 60));

        resignBtn = new JButton("投了");
        resignBtn.setForeground(Color.RED);
        resignBtn.addActionListener(e -> onResign());

        lobbyButton = new JButton("ロビーに戻る");
        lobbyButton.setEnabled(false);
        lobbyButton.addActionListener(e -> mainFrame.showLobby());

        panel.add(opponentInfoLabel);
        panel.add(myInfoLabel);
        panel.add(turnLabel);
        panel.add(timerLabel);
        panel.add(resignBtn);
        panel.add(lobbyButton);

        return panel;
    }

    // Chat panel
    private JPanel buildChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createTitledBorder("チャット"));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        chatInput = new JTextField();
        JButton sendBtn = new JButton("送信");

        // Both send button and Enter key trigger send
        sendBtn.addActionListener(e -> sendChat());
        chatInput.addActionListener(e -> sendChat());

        JPanel inputRow = new JPanel(new BorderLayout(4, 0));
        inputRow.add(chatInput, BorderLayout.CENTER);
        inputRow.add(sendBtn, BorderLayout.EAST);

        panel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        panel.add(inputRow, BorderLayout.SOUTH);

        return panel;
    }

    // ── Server message handlers (called by MainFrame) ──

    // Handle confirmed move - place stone on board
    public void onMoveReceived(int row, int col, int color) {
        boardPanel.placeStone(row, col, color);

        boolean isMyMove = (color == myColor);
        boolean nowMyTurn = !isMyMove;  // If opponent moved, now it's my turn

        boardPanel.setMyTurn(nowMyTurn);
        updateTurnLabel(nowMyTurn);

        if (nowMyTurn) startTimer();
        else stopTimer();
    }

    // Handle game over notification
    public void onGameOver(String result) {
        stopTimer();
        boardPanel.setMyTurn(false);
        lobbyButton.setEnabled(true);
        resignBtn.setEnabled(false);

        String message;
        if (result.equals("WIN"))       message = "🎉 勝利しました！";
        else if (result.equals("LOSE")) message = "😢 敗北しました。";
        else                            message = "🤝 引き分けです。";

        turnLabel.setText(message);

        // Show result dialog after brief delay
        Timer delay = new Timer(500, e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    message + "\nロビーに戻りますか？", "ゲーム終了",
                    JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                mainFrame.showLobby();
            }
        });
        delay.setRepeats(false);
        delay.start();
    }

    // Handle incoming chat message
    public void onChatReceived(String nickname, String content) {
        chatArea.append(nickname + ": " + content + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    // Highlight winning stones
    public void onWinHighlight(int[][] stones) {
        boardPanel.highlightWin(stones);
    }

    // ── Game initialization ──
    public void startGame(UserVO user, String opponentNick, int color) {
        this.myUser            = user;
        this.opponentNickname  = opponentNick;
        this.myColor           = color;

        boardPanel.reset();
        chatArea.setText("");
        lobbyButton.setEnabled(false);
        resignBtn.setEnabled(true);

        String colorStr = (color == 1) ? "黒石 ●" : "白石 ○";
        myInfoLabel.setText("自分: " + user.getNickname() + " (" + colorStr + ")");
        opponentInfoLabel.setText("相手: " + opponentNick);

        // Black stone goes first
        boolean myFirst = (color == 1);
        boardPanel.setMyTurn(myFirst);
        updateTurnLabel(myFirst);

        if (myFirst) startTimer();

        addSystemChat("ゲームが開始されました。" +
                (myFirst ? "黒石で先手です。" : "白石です。相手の手番をお待ちください。"));
    }

    // ── Internal helpers ──

    private void sendChat() {
        String text = chatInput.getText().trim();
        if (text.isEmpty()) return;
        mainFrame.sendToServer(MessageProtocol.build(MessageProtocol.CHAT, text));
        chatInput.setText("");
    }

    private void onResign() {
        int choice = JOptionPane.showConfirmDialog(this,
                "本当に投了しますか？", "投了", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            mainFrame.sendToServer(MessageProtocol.RESIGN);
            mainFrame.showLobby();
        }
    }

    private void addSystemChat(String msg) {
        chatArea.append("[システム] " + msg + "\n");
    }

    private void updateTurnLabel(boolean myTurn) {
        if (myTurn) {
            turnLabel.setText("● 自分の番");
            turnLabel.setForeground(new Color(0, 120, 0));
        } else {
            turnLabel.setText("○ 相手の番");
            turnLabel.setForeground(new Color(100, 100, 100));
        }
    }

    // 30-second countdown timer
    private void startTimer() {
        stopTimer();
        timeLeft = 30;
        timerLabel.setText("⏱ " + timeLeft + "秒");

        countdownTimer = new Timer(1000, e -> {
            timeLeft--;
            timerLabel.setText("⏱ " + timeLeft + "秒");
            timerLabel.setForeground(timeLeft <= 10
                    ? new Color(200, 0, 0) : new Color(180, 60, 60));

            if (timeLeft <= 0) {
                stopTimer();
                mainFrame.sendToServer(MessageProtocol.TIMEOUT);
            }
        });
        countdownTimer.start();
    }

    private void stopTimer() {
        if (countdownTimer != null) {
            countdownTimer.stop();
            countdownTimer = null;
        }
        timerLabel.setText("⏱ --秒");
    }
}