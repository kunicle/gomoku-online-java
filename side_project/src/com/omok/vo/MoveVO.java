package com.omok.vo;

import java.time.LocalDateTime;

/**
 * Value Object mapping to the moves table.
 * Records each stone placement with coordinates, color, and think time.
 * think_time_ms serves as feature data for future recommendation system training.
 */
public class MoveVO {

    private final long moveId;
    private final long gameId;
    private final long playerId;    // 0 for AI moves
    private final int moveNumber;   // Move sequence number (starts at 1)
    private final int rowPos;       // Row coordinate (0-18)
    private final int colPos;       // Column coordinate (0-18)
    private final String stoneColor; // BLACK / WHITE
    private final int thinkTimeMs;  // Time taken to place stone (ms)
    private final LocalDateTime placedAt;

    public MoveVO(long moveId, long gameId, long playerId, int moveNumber,
                  int rowPos, int colPos, String stoneColor,
                  int thinkTimeMs, LocalDateTime placedAt) {
        this.moveId      = moveId;
        this.gameId      = gameId;
        this.playerId    = playerId;
        this.moveNumber  = moveNumber;
        this.rowPos      = rowPos;
        this.colPos      = colPos;
        this.stoneColor  = stoneColor;
        this.thinkTimeMs = thinkTimeMs;
        this.placedAt    = placedAt;
    }

    public long getMoveId()            { return moveId; }
    public long getGameId()            { return gameId; }
    public long getPlayerId()          { return playerId; }
    public int getMoveNumber()         { return moveNumber; }
    public int getRowPos()             { return rowPos; }
    public int getColPos()             { return colPos; }
    public String getStoneColor()      { return stoneColor; }
    public int getThinkTimeMs()        { return thinkTimeMs; }
    public LocalDateTime getPlacedAt() { return placedAt; }
}