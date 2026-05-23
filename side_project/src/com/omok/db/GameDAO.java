package com.omok.db;

import com.omok.vo.GameVO;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GameDAO {

    private final Connection conn;

    public GameDAO() throws SQLException {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // Insert game start record - returns generated game_id
    public long insert(long player1Id, long player2Id) throws SQLException {
        String sql = "INSERT INTO games (player1_id, player2_id, started_at) VALUES (?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, player1Id);
            ps.setLong(2, player2Id);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            // Return the AUTO_INCREMENT generated game_id
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Failed to generate game_id");
    }

    // Insert solo game (AI battle) - player2_id is NULL
    public long insertSolo(long player1Id) throws SQLException {
        String sql = "INSERT INTO games (player1_id, started_at) VALUES (?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, player1Id);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Failed to generate game_id");
    }

    // Update game result when game ends
    public void update(long gameId, long winnerId, String result,
                       int totalMoves, String winPositions) throws SQLException {
        String sql = "UPDATE games SET winner_id=?, result=?, total_moves=?, " +
                "win_positions=?, ended_at=? WHERE game_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // winner_id is NULL for draws
            if (winnerId > 0) ps.setLong(1, winnerId);
            else              ps.setNull(1, Types.BIGINT);

            ps.setString(2, result);
            ps.setInt(3, totalMoves);
            ps.setString(4, winPositions);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(6, gameId);
            ps.executeUpdate();
        }
    }

    // Retrieve games by user_id (used for replay list)
    public List<GameVO> findByUserId(long userId) throws SQLException {
        String sql = "SELECT * FROM games WHERE player1_id=? OR player2_id=? " +
                "ORDER BY started_at DESC";
        List<GameVO> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapToVO(rs));
            }
        }
        return list;
    }

    private GameVO mapToVO(ResultSet rs) throws SQLException {
        return new GameVO(
                rs.getLong("game_id"),
                rs.getLong("player1_id"),
                rs.getLong("player2_id"),
                rs.getLong("winner_id"),
                rs.getString("result"),
                rs.getInt("total_moves"),
                rs.getString("win_positions"),
                rs.getTimestamp("started_at") != null
                        ? rs.getTimestamp("started_at").toLocalDateTime() : null,
                rs.getTimestamp("ended_at") != null
                        ? rs.getTimestamp("ended_at").toLocalDateTime() : null
        );
    }
}