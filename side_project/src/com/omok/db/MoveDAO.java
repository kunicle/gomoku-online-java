package com.omok.db;

import com.omok.vo.MoveVO;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MoveDAO {

    private final Connection conn;

    public MoveDAO() throws SQLException {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // Insert a single move record (player move)
    public void insert(long gameId, long playerId, int moveNumber,
                       int row, int col, String stoneColor,
                       int thinkTimeMs) throws SQLException {
        String sql = "INSERT INTO moves (game_id, player_id, move_number, " +
                "row_pos, col_pos, stone_color, think_time_ms, placed_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, gameId);
            ps.setLong(2, playerId);
            ps.setInt(3, moveNumber);
            ps.setInt(4, row);
            ps.setInt(5, col);
            ps.setString(6, stoneColor);
            ps.setInt(7, thinkTimeMs);
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    // Insert AI move record - player_id is NULL (AI is not a registered user)
    public void insertAI(long gameId, int moveNumber, int row, int col,
                         String stoneColor, int thinkTimeMs) throws SQLException {
        String sql = "INSERT INTO moves (game_id, move_number, row_pos, col_pos, " +
                "stone_color, think_time_ms, placed_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, gameId);
            ps.setInt(2, moveNumber);
            ps.setInt(3, row);
            ps.setInt(4, col);
            ps.setString(5, stoneColor);
            ps.setInt(6, thinkTimeMs);
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    // Retrieve all move records for a game (used for replay)
    public List<MoveVO> findByGameId(long gameId) throws SQLException {
        String sql = "SELECT * FROM moves WHERE game_id=? ORDER BY move_number ASC";
        List<MoveVO> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapToVO(rs));
            }
        }
        return list;
    }

    private MoveVO mapToVO(ResultSet rs) throws SQLException {
        return new MoveVO(
                rs.getLong("move_id"),
                rs.getLong("game_id"),
                rs.getLong("player_id"),
                rs.getInt("move_number"),
                rs.getInt("row_pos"),
                rs.getInt("col_pos"),
                rs.getString("stone_color"),
                rs.getInt("think_time_ms"),
                rs.getTimestamp("placed_at").toLocalDateTime()
        );
    }
}