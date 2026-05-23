package com.omok.vo;

import java.time.LocalDateTime;

/**
 * Value Object mapping to the games table.
 * Stores game metadata including result and winning stone positions.
 */
public class GameVO {

    private final long gameId;
    private final long player1Id;
    private final long player2Id;   // 0 for solo (AI) games
    private final long winnerId;    // 0 for draws
    private final String result;    // BLACK_WIN / WHITE_WIN / DRAW
    private final int totalMoves;
    private final String winPositions; // "r1,c1;r2,c2;..." for replay highlight
    private final LocalDateTime startedAt;
    private final LocalDateTime endedAt;

    public GameVO(long gameId, long player1Id, long player2Id, long winnerId,
                  String result, int totalMoves, String winPositions,
                  LocalDateTime startedAt, LocalDateTime endedAt) {
        this.gameId        = gameId;
        this.player1Id     = player1Id;
        this.player2Id     = player2Id;
        this.winnerId      = winnerId;
        this.result        = result;
        this.totalMoves    = totalMoves;
        this.winPositions  = winPositions;
        this.startedAt     = startedAt;
        this.endedAt       = endedAt;
    }

    public long getGameId()             { return gameId; }
    public long getPlayer1Id()          { return player1Id; }
    public long getPlayer2Id()          { return player2Id; }
    public long getWinnerId()           { return winnerId; }
    public String getResult()           { return result; }
    public int getTotalMoves()          { return totalMoves; }
    public String getWinPositions()     { return winPositions; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEndedAt()   { return endedAt; }
}