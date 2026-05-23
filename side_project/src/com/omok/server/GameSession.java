package com.omok.server;

import com.omok.common.MessageProtocol;
import com.omok.db.GameDAO;
import com.omok.db.MoveDAO;
import com.omok.db.UserDAO;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages the state of a single game session.
 * Handles board state, turn switching, win detection, timer, and DB persistence.
 */
public class GameSession {

    private static final int BOARD_SIZE = 19;

    // Board state: 0=empty, 1=black, 2=white
    private final int[][] board = new int[BOARD_SIZE][BOARD_SIZE];

    private final ClientHandler blackPlayer;  // Black stone (goes first)
    private final ClientHandler whitePlayer;  // White stone

    // Current turn: 1=black, 2=white
    private int currentTurn = 1;
    private int moveCount = 0;
    private boolean gameOver = false;

    // Track last stone position for win position calculation
    private int lastRow = -1, lastCol = -1;

    // DB fields
    private long gameId;
    private long blackPlayerId;
    private long whitePlayerId;
    private long turnStartTime;

    // 30-second move timer
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> turnTimer;

    public GameSession(ClientHandler blackPlayer, ClientHandler whitePlayer) {
        this.blackPlayer   = blackPlayer;
        this.whitePlayer   = whitePlayer;
        blackPlayer.setCurrentGame(this);
        whitePlayer.setCurrentGame(this);

        this.blackPlayerId = blackPlayer.getUser() != null ? blackPlayer.getUser().getUserId() : 0;
        this.whitePlayerId = whitePlayer.getUser() != null ? whitePlayer.getUser().getUserId() : 0;
        this.turnStartTime = System.currentTimeMillis();

        // Save game start to DB
        try {
            GameDAO gameDAO = new GameDAO();
            this.gameId = gameDAO.insert(blackPlayerId, whitePlayerId);
            System.out.println("Game saved to DB - game_id: " + gameId);
        } catch (Exception e) {
            System.out.println("Failed to save game to DB: " + e.getMessage());
        }

        startTurnTimer(blackPlayer);
    }

    // Process a stone placement request
    public synchronized void placeStone(ClientHandler handler, int row, int col) {
        if (gameOver) return;

        // Validate turn
        int playerColor = (handler == blackPlayer) ? 1 : 2;
        if (playerColor != currentTurn) {
            handler.send(MessageProtocol.build(MessageProtocol.ERROR, "Not your turn."));
            return;
        }

        // Validate position
        if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE || board[row][col] != 0) {
            handler.send(MessageProtocol.build(MessageProtocol.ERROR, "Invalid move."));
            return;
        }

        // Place stone
        board[row][col] = currentTurn;
        lastRow = row;
        lastCol = col;
        moveCount++;
        stopTurnTimer();

        // Broadcast move result to both players
        String colorStr = (currentTurn == 1) ? "BLACK" : "WHITE";
        broadcastToPlayers(MessageProtocol.build(MessageProtocol.MOVE_OK,
                String.valueOf(row), String.valueOf(col), colorStr));

        // Save move to DB
        long playerId = (currentTurn == 1) ? blackPlayerId : whitePlayerId;
        int thinkTimeMs = (int)(System.currentTimeMillis() - turnStartTime);
        turnStartTime = System.currentTimeMillis();

        try {
            MoveDAO moveDAO = new MoveDAO();
            moveDAO.insert(gameId, playerId, moveCount, row, col, colorStr, thinkTimeMs);
        } catch (Exception e) {
            System.out.println("Failed to save move to DB: " + e.getMessage());
        }

        // Check win condition
        if (checkWin(row, col)) {
            endGame(handler, getOpponent(handler));
            return;
        }

        // Check draw (all 361 cells filled)
        if (moveCount == BOARD_SIZE * BOARD_SIZE) {
            blackPlayer.send(MessageProtocol.build(MessageProtocol.GAME_OVER, "DRAW"));
            whitePlayer.send(MessageProtocol.build(MessageProtocol.GAME_OVER, "DRAW"));
            gameOver = true;
            scheduler.shutdown();
            return;
        }

        // Switch turn
        currentTurn = (currentTurn == 1) ? 2 : 1;
        ClientHandler nextPlayer = (currentTurn == 1) ? blackPlayer : whitePlayer;
        startTurnTimer(nextPlayer);
    }

    /**
     * Checks win condition from the last placed stone.
     * Searches 4 directions (horizontal, vertical, diagonal) for 5 consecutive stones.
     * Only checks from the last move position - O(1) performance.
     */
    private boolean checkWin(int row, int col) {
        int color = board[row][col];
        // Directions: horizontal(0,1) | vertical(1,0) | diagonal-right(1,1) | diagonal-left(1,-1)
        int[][] directions = {{0,1}, {1,0}, {1,1}, {1,-1}};

        for (int[] dir : directions) {
            int count = 1;  // Include current stone
            // Count stones in both directions and sum
            count += countStones(row, col,  dir[0],  dir[1], color);
            count += countStones(row, col, -dir[0], -dir[1], color);
            if (count >= 5) return true;
        }
        return false;
    }

    // Count consecutive same-color stones in one direction
    private int countStones(int row, int col, int dr, int dc, int color) {
        int count = 0;
        int r = row + dr;
        int c = col + dc;

        while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE
                && board[r][c] == color) {
            count++;
            r += dr;
            c += dc;
        }
        return count;
    }

    // Handle game end - update DB and notify players
    private void endGame(ClientHandler winner, ClientHandler loser) {
        gameOver = true;
        scheduler.shutdown();

        winner.setLastOpponent(loser);
        loser.setLastOpponent(winner);

        winner.send(MessageProtocol.build(MessageProtocol.GAME_OVER, "WIN"));
        loser.send(MessageProtocol.build(MessageProtocol.GAME_OVER, "LOSE"));

        long winnerId = winner.getUser() != null ? winner.getUser().getUserId() : 0;
        boolean winnerIsBlack = (winner == blackPlayer);
        String result = winnerIsBlack ? "BLACK_WIN" : "WHITE_WIN";
        String winPos = buildWinPositions(board, winnerIsBlack ? 1 : 2, lastRow, lastCol);

        try {
            GameDAO gameDAO = new GameDAO();
            gameDAO.update(gameId, winnerId, result, moveCount, winPos);

            UserDAO userDAO = new UserDAO();
            userDAO.updateRecord(winnerId, "WIN");
            userDAO.updateRecord(
                    winner == blackPlayer ? whitePlayerId : blackPlayerId, "LOSE");

            System.out.println("Game result saved to DB");
        } catch (Exception e) {
            System.out.println("Failed to save game result: " + e.getMessage());
        }

        winner.setCurrentGame(null);
        loser.setCurrentGame(null);
    }

    // Broadcast chat message to both players
    public void broadcastChat(ClientHandler sender, String content) {
        String nickname = sender.getUser() != null ? sender.getUser().getNickname() : "unknown";
        broadcastToPlayers(MessageProtocol.build(MessageProtocol.CHAT, nickname, content));
    }

    // Handle player disconnection during game
    public void onPlayerDisconnect(ClientHandler handler) {
        if (gameOver) return;
        gameOver = true;
        stopTurnTimer();
        scheduler.shutdown();

        ClientHandler opponent = getOpponent(handler);
        if (opponent != null) {
            handler.setLastOpponent(opponent);
            opponent.setLastOpponent(handler);
            opponent.send(MessageProtocol.build(MessageProtocol.GAME_OVER, "WIN"));
            opponent.setCurrentGame(null);

            long winnerId = opponent.getUser() != null ? opponent.getUser().getUserId() : 0;
            long loserId  = handler.getUser()  != null ? handler.getUser().getUserId()  : 0;
            boolean winnerIsBlack = (opponent == blackPlayer);
            String result = winnerIsBlack ? "BLACK_WIN" : "WHITE_WIN";

            try {
                GameDAO gameDAO = new GameDAO();
                gameDAO.update(gameId, winnerId, result, moveCount, "");

                UserDAO userDAO = new UserDAO();
                userDAO.updateRecord(winnerId, "WIN");
                userDAO.updateRecord(loserId,  "LOSE");

                System.out.println("Resign result saved to DB");
            } catch (Exception e) {
                System.out.println("Failed to save resign result: " + e.getMessage());
            }
        }
        handler.setCurrentGame(null);
    }

    // Start 30-second turn timer
    private void startTurnTimer(ClientHandler player) {
        turnTimer = scheduler.schedule(() -> {
            if (!gameOver) {
                player.send(MessageProtocol.build(MessageProtocol.GAME_OVER, "LOSE"));
                ClientHandler opponent = getOpponent(player);
                if (opponent != null) {
                    opponent.send(MessageProtocol.build(MessageProtocol.GAME_OVER, "WIN"));
                }
                gameOver = true;
            }
        }, 35, TimeUnit.SECONDS);  // 30s client timer + 5s network buffer
    }

    private void stopTurnTimer() {
        if (turnTimer != null && !turnTimer.isDone()) {
            turnTimer.cancel(false);
        }
    }

    // Broadcast a message to both players
    public void broadcastToPlayers(String message) {
        blackPlayer.send(message);
        whitePlayer.send(message);
    }

    private ClientHandler getOpponent(ClientHandler player) {
        return (player == blackPlayer) ? whitePlayer : blackPlayer;
    }

    /**
     * Builds a win position string from the winning 5 stones.
     * Format: "r1,c1;r2,c2;r3,c3;r4,c4;r5,c5"
     * Used for replay highlight display.
     */
    private String buildWinPositions(int[][] board, int color, int row, int col) {
        int[][] directions = {{0,1},{1,0},{1,1},{1,-1}};
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
        return "";
    }
}