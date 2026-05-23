package com.omok.db;

import com.omok.vo.GameStatsVO;

import java.sql.*;

public class StatsDAO {

    private final Connection conn;

    public StatsDAO() throws SQLException {
        this.conn = DBConnection.getInstance().getConnection();
    }

    /**
     * Retrieves aggregated game statistics for a user.
     * Uses JOIN queries to combine data from games and moves tables.
     *
     * @param userId Target user ID
     * @return GameStatsVO containing win rate, avg think time, etc.
     */
    public GameStatsVO getStatsByUserId(long userId) throws SQLException {

        // ── Query 1: Aggregate games table stats ──
        // CASE WHEN counts only winning games
        // AVG with CASE WHEN calculates average moves only for won games
        String gamesSql =
                "SELECT " +
                        "  COUNT(*) AS total_games, " +
                        "  SUM(CASE WHEN winner_id = ? THEN 1 ELSE 0 END) AS wins, " +
                        "  AVG(total_moves) AS avg_total_moves, " +
                        "  AVG(CASE WHEN winner_id = ? THEN total_moves ELSE NULL END) AS winning_move_avg " +
                        "FROM games " +
                        "WHERE player1_id = ? OR player2_id = ?";

        int totalGames = 0;
        double winRate = 0;
        double avgTotalMoves = 0;
        double winningMoveAvg = 0;

        try (PreparedStatement ps = conn.prepareStatement(gamesSql)) {
            // Bind in order:
            // 1: winner_id = userId (for win count)
            // 2: winner_id = userId (for winning move avg)
            // 3: player1_id = userId
            // 4: player2_id = userId
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ps.setLong(3, userId);
            ps.setLong(4, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalGames = rs.getInt("total_games");
                    int wins = rs.getInt("wins");
                    winRate = totalGames > 0 ? (double) wins / totalGames : 0;
                    avgTotalMoves  = rs.getDouble("avg_total_moves");
                    winningMoveAvg = rs.getDouble("winning_move_avg");
                }
            }
        }

        // ── Query 2: moves LEFT JOIN games ──
        // LEFT JOIN connects moves and games tables via game_id
        // AI moves (player_id = NULL) are excluded by WHERE m.player_id = ?
        String movesSql =
                "SELECT AVG(m.think_time_ms) AS avg_think_time " +
                        "FROM moves m " +
                        "LEFT JOIN games g ON m.game_id = g.game_id " +
                        "WHERE m.player_id = ?";

        double avgThinkTimeMs = 0;

        try (PreparedStatement ps = conn.prepareStatement(movesSql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    avgThinkTimeMs = rs.getDouble("avg_think_time");
                }
            }
        }

        return new GameStatsVO(userId, winRate, avgThinkTimeMs,
                avgTotalMoves, winningMoveAvg, totalGames);
    }
}