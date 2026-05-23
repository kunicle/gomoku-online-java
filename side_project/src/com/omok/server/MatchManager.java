package com.omok.server;

import com.omok.common.MessageProtocol;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Singleton class that manages the matchmaking queue.
 * When 2 players are in the queue, creates a GameSession and notifies both clients.
 */
public class MatchManager {

    private static MatchManager instance;

    // Thread-safe queue: safe for concurrent access by multiple ClientHandler threads
    private final ConcurrentLinkedQueue<ClientHandler> waitingQueue;

    private MatchManager() {
        waitingQueue = new ConcurrentLinkedQueue<>();
    }

    public static synchronized MatchManager getInstance() {
        if (instance == null) {
            instance = new MatchManager();
        }
        return instance;
    }

    // Add player to queue and attempt matching
    public synchronized void addToQueue(ClientHandler handler) {
        // Prevent duplicate entries
        if (waitingQueue.contains(handler)) return;

        waitingQueue.add(handler);
        System.out.println("Added to queue: " +
                (handler.getUser() != null ? handler.getUser().getNickname() : "not logged in") +
                " (waiting: " + waitingQueue.size() + ")");

        tryMatch();
    }

    // Remove player from queue
    public synchronized void removeFromQueue(ClientHandler handler) {
        waitingQueue.remove(handler);
        System.out.println("Removed from queue (waiting: " + waitingQueue.size() + ")");
    }

    // Match first two players in queue (FIFO)
    private void tryMatch() {
        if (waitingQueue.size() < 2) return;

        ClientHandler player1 = waitingQueue.poll(); // Black stone (goes first)
        ClientHandler player2 = waitingQueue.poll(); // White stone

        System.out.println("Match found: " +
                (player1.getUser() != null ? player1.getUser().getNickname() : "player1") +
                " vs " +
                (player2.getUser() != null ? player2.getUser().getNickname() : "player2"));

        // Create game session
        GameSession session = new GameSession(player1, player2);

        // Notify both players of match result
        // MATCH_FOUND|opponentNickname|myColor
        String p1Nick = player1.getUser() != null ? player1.getUser().getNickname() : "player1";
        String p2Nick = player2.getUser() != null ? player2.getUser().getNickname() : "player2";

        player1.send(MessageProtocol.build(MessageProtocol.MATCH_FOUND, p2Nick, "BLACK"));
        player2.send(MessageProtocol.build(MessageProtocol.MATCH_FOUND, p1Nick, "WHITE"));
    }
}