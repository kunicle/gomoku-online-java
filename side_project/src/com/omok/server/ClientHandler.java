package com.omok.server;

import com.omok.common.MessageProtocol;
import com.omok.vo.UserVO;

import java.io.*;
import java.net.Socket;

/**
 * One ClientHandler thread is created per connected client.
 * Responsible for receiving, parsing, and routing messages from the client.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // User info set after successful login
    private UserVO user;

    // Current game session (null before matchmaking)
    private GameSession currentGame;

    // Last opponent for post-game chat
    private ClientHandler lastOpponent;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            // Wrap socket I/O streams as text-based readers/writers
            // BufferedReader: reads line by line
            // PrintWriter: writes line by line (autoFlush=true for immediate send)
            this.in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        } catch (IOException e) {
            System.out.println("ClientHandler init failed: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        System.out.println("Client connected: " + socket.getInetAddress());
        try {
            String message;
            // Continuously read messages from client line by line
            // readLine() returns null when client disconnects
            while ((message = in.readLine()) != null) {
                handleMessage(message);
            }
        } catch (IOException e) {
            System.out.println("Client connection lost: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    // Routes incoming messages by type
    private void handleMessage(String message) {
        System.out.println("Received: " + message);
        String[] parts = MessageProtocol.parse(message);
        String type = parts[0];

        switch (type) {
            case MessageProtocol.PING:
                send(MessageProtocol.PONG);
                break;
            case MessageProtocol.LOGIN:
                // LOGIN|username|nickname|userId
                // Registers user info to this handler after socket connection
                try {
                    long userId = Long.parseLong(parts[3]);
                    UserVO loginUser = new UserVO(
                            userId, parts[1], "", parts[2], 0, 0, 0, null, null
                    );
                    this.user = loginUser;
                    System.out.println("User registered: " + parts[2]);
                } catch (Exception e) {
                    System.out.println("User registration failed: " + e.getMessage());
                }
                break;
            case MessageProtocol.MATCH_REQUEST:
                MatchManager.getInstance().addToQueue(this);
                break;
            case MessageProtocol.MATCH_CANCEL:
                MatchManager.getInstance().removeFromQueue(this);
                break;
            case MessageProtocol.MOVE:
                if (currentGame != null) {
                    int row = Integer.parseInt(parts[1]);
                    int col = Integer.parseInt(parts[2]);
                    currentGame.placeStone(this, row, col);
                }
                break;
            case MessageProtocol.CHAT:
                if (currentGame != null) {
                    currentGame.broadcastChat(this, parts[1]);
                } else if (lastOpponent != null) {
                    // Allow chat with last opponent after game ends
                    String nickname = user != null ? user.getNickname() : "unknown";
                    String msg = MessageProtocol.build(MessageProtocol.CHAT, nickname, parts[1]);
                    send(msg);
                    lastOpponent.send(msg);
                }
                break;
            case MessageProtocol.RESIGN:
                // Resign: end game session but keep socket connection
                if (currentGame != null) {
                    currentGame.onPlayerDisconnect(this);
                    currentGame = null;
                }
                break;
            case MessageProtocol.DISCONNECT:
                disconnect();
                break;
            default:
                System.out.println("Unknown message type: " + type);
        }
    }

    // Send a message to this client
    public void send(String message) {
        out.println(message);
    }

    // Handle disconnection
    public void disconnect() {
        try {
            if (currentGame != null) {
                currentGame.onPlayerDisconnect(this);
                currentGame = null;
            }
            MatchManager.getInstance().removeFromQueue(this);
            socket.close();
            System.out.println("Client disconnected: " +
                    (user != null ? user.getNickname() : "not logged in"));
        } catch (IOException e) {
            System.out.println("Socket close error: " + e.getMessage());
        }
    }

    public UserVO getUser()                     { return user; }
    public void setUser(UserVO user)            { this.user = user; }
    public GameSession getCurrentGame()         { return currentGame; }
    public void setCurrentGame(GameSession g)   { this.currentGame = g; }
    public void setLastOpponent(ClientHandler h){ this.lastOpponent = h; }
}