package com.omok.vo;

/**
 * Value Object for aggregated game statistics.
 * Populated by StatsDAO and passed to StatsPanel for display.
 * Fields represent potential features for a recommendation system.
 */
public class GameStatsVO {

    private final long userId;
    private final double winRate;         // Win rate (0.0 ~ 1.0)
    private final double avgThinkTimeMs;  // Average think time per move (ms)
    private final double avgTotalMoves;   // Average game length (moves)
    private final double winningMoveAvg;  // Average move count in won games
    private final int totalGames;

    public GameStatsVO(long userId, double winRate, double avgThinkTimeMs,
                       double avgTotalMoves, double winningMoveAvg, int totalGames) {
        this.userId         = userId;
        this.winRate        = winRate;
        this.avgThinkTimeMs = avgThinkTimeMs;
        this.avgTotalMoves  = avgTotalMoves;
        this.winningMoveAvg = winningMoveAvg;
        this.totalGames     = totalGames;
    }

    public long getUserId()           { return userId; }
    public double getWinRate()        { return winRate; }
    public double getAvgThinkTimeMs() { return avgThinkTimeMs; }
    public double getAvgTotalMoves()  { return avgTotalMoves; }
    public double getWinningMoveAvg() { return winningMoveAvg; }
    public int getTotalGames()        { return totalGames; }
}