package com.omok.client;

import com.omok.common.MessageProtocol;
import com.omok.vo.UserVO;

import javax.swing.*;
import java.awt.*;

/**
 * Main application window.
 * Manages screen transitions between panels using CardLayout.
 * Holds the NetworkClient and current user session.
 */
public class MainFrame extends JFrame {

    // CardLayout keys for each panel
    private static final String CARD_LOGIN  = "LOGIN";
    private static final String CARD_LOBBY  = "LOBBY";
    private static final String CARD_GAME   = "GAME";
    private static final String CARD_STATS  = "STATS";
    private static final String CARD_REPLAY = "REPLAY";
    private static final String CARD_SOLO   = "SOLO";

    // Stacks panels like cards - shows one at a time
    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    // Current logged-in user (set after successful login)
    private UserVO currentUser;

    // Individual panels
    private LobbyPanel lobbyPanel;
    private GamePanel gamePanel;
    private StatsPanel statsPanel;
    private ReplayPanel replayPanel;
    private SoloGamePanel soloGamePanel;

    // Socket connection to server
    private NetworkClient networkClient;

    // Flask AI server process
    private Process flaskProcess;

    public MainFrame() {
        setTitle("五目並べオンライン");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 820);
        setLocationRelativeTo(null);  // Center on screen
        setResizable(false);

        // Start Flask AI server automatically
        startFlaskServer();

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);

        // Register all panels
        cardPanel.add(new LoginPanel(this), CARD_LOGIN);
        lobbyPanel     = new LobbyPanel(this);
        gamePanel      = new GamePanel(this);
        statsPanel     = new StatsPanel(this);
        replayPanel    = new ReplayPanel(this);
        soloGamePanel  = new SoloGamePanel(this);

        cardPanel.add(lobbyPanel,    CARD_LOBBY);
        cardPanel.add(gamePanel,     CARD_GAME);
        cardPanel.add(statsPanel,    CARD_STATS);
        cardPanel.add(replayPanel,   CARD_REPLAY);
        cardPanel.add(soloGamePanel, CARD_SOLO);

        add(cardPanel);

        // Connect to game server
        networkClient = new NetworkClient(this::onMessageReceived);
        if (!networkClient.connect()) {
            JOptionPane.showMessageDialog(null,
                    "Cannot connect to server.\nPlease start the server first.");
        }

        // Show login screen on startup
        cardLayout.show(cardPanel, CARD_LOGIN);
    }

    // ── Server message routing ──
    // Routes incoming server messages to the appropriate handler
    private void onMessageReceived(String message) {
        System.out.println("Message received: " + message);
        String[] parts = message.split("\\|");
        String type = parts[0];

        switch (type) {
            case MessageProtocol.PONG:
                System.out.println("Server connection OK");
                break;
            case MessageProtocol.MATCH_FOUND:
                // MATCH_FOUND|opponentNick|myColor
                lobbyPanel.onMatchFound();
                String opponentNick = parts[1];
                int myColor = parts[2].equals("BLACK") ? 1 : 2;
                gamePanel.startGame(currentUser, opponentNick, myColor);
                cardLayout.show(cardPanel, CARD_GAME);
                break;
            case MessageProtocol.MOVE_OK:
                // MOVE_OK|row|col|color
                int row   = Integer.parseInt(parts[1]);
                int col   = Integer.parseInt(parts[2]);
                int color = parts[3].equals("BLACK") ? 1 : 2;
                gamePanel.onMoveReceived(row, col, color);
                break;
            case MessageProtocol.GAME_OVER:
                // GAME_OVER|WIN or LOSE or DRAW
                gamePanel.onGameOver(parts[1]);
                break;
            case MessageProtocol.CHAT:
                // CHAT|nickname|content
                gamePanel.onChatReceived(parts[1], parts[2]);
                break;
            default:
                System.out.println("Unhandled message: " + message);
        }
    }

    // ── Screen transition methods ──

    // Called by LoginPanel on successful login
    public void onLoginSuccess(UserVO user) {
        this.currentUser = user;
        // Send user info to server for registration
        networkClient.send(MessageProtocol.build(
                MessageProtocol.LOGIN,
                user.getUsername(),
                user.getNickname(),
                String.valueOf(user.getUserId())
        ));
        lobbyPanel.refresh(user);
        cardLayout.show(cardPanel, CARD_LOBBY);
    }

    // Return to lobby and refresh user data from DB
    public void showLobby() {
        try {
            com.omok.db.UserDAO dao = new com.omok.db.UserDAO();
            UserVO updated = dao.findByUserId(currentUser.getUserId());
            if (updated != null) currentUser = updated;
        } catch (Exception e) {
            System.out.println("Failed to refresh user data: " + e.getMessage());
        }
        lobbyPanel.refresh(currentUser);
        cardLayout.show(cardPanel, CARD_LOBBY);
    }

    public void showStats() {
        statsPanel.refresh(currentUser);
        cardLayout.show(cardPanel, CARD_STATS);
    }

    public void showReplay() {
        replayPanel.refresh(currentUser);
        cardLayout.show(cardPanel, CARD_REPLAY);
    }

    public void showSolo() {
        SoloStartDialog dialog = new SoloStartDialog(this);
        dialog.setVisible(true);

        int selectedColor = dialog.getSelectedColor();
        if (selectedColor == -1) return;  // Cancelled

        soloGamePanel.startGame(currentUser, selectedColor);
        cardLayout.show(cardPanel, CARD_SOLO);
    }

    // Send message to game server
    public void sendToServer(String message) {
        networkClient.send(message);
    }

    // ── Flask AI server management ──

    private void startFlaskServer() {
        try {
            // Skip if Flask is already running on port 5001
            if (isFlaskRunning()) {
                System.out.println("Flask server already running");
                return;
            }

            // Use python3 on Mac, python on Windows
            String pythonCmd = System.getProperty("os.name").toLowerCase().contains("win")
                    ? "python" : "python3";
            ProcessBuilder pb = new ProcessBuilder(pythonCmd, "omok_ai/app.py");
            pb.redirectErrorStream(true);
            flaskProcess = pb.start();
            System.out.println("Flask server starting...");

            // Wait for server to be ready
            Thread.sleep(3000);
            System.out.println("Flask server ready");

        } catch (Exception e) {
            System.out.println("Flask server start failed: " + e.getMessage());
        }
    }

    // Check if port 5001 is already in use
    private boolean isFlaskRunning() {
        try {
            java.net.Socket socket = new java.net.Socket("127.0.0.1", 5001);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public UserVO getCurrentUser()         { return currentUser; }
    public NetworkClient getNetworkClient(){ return networkClient; }

    // Application entry point
    public static void main(String[] args) {
        // Swing UI must run on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            // Terminate Flask process when Java exits
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (frame.flaskProcess != null) {
                    frame.flaskProcess.destroy();
                    System.out.println("Flask server stopped");
                }
            }));
            frame.setVisible(true);
        });
    }
}