package com.omok.db;

import com.omok.vo.ChatMessageVO;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatMessageDAO {

    private final Connection conn;

    public ChatMessageDAO() throws SQLException {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // Insert a chat message record
    public void insert(ChatMessageVO msg) throws SQLException {
        String sql = "INSERT INTO chat_messages (game_id, sender_id, content, sent_at) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, msg.getGameId());
            ps.setLong(2, msg.getSenderId());
            ps.setString(3, msg.getContent());
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    // Retrieve all chat messages for a game
    public List<ChatMessageVO> findByGameId(long gameId) throws SQLException {
        String sql = "SELECT * FROM chat_messages WHERE game_id=? ORDER BY sent_at ASC";
        List<ChatMessageVO> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapToVO(rs));
            }
        }
        return list;
    }

    private ChatMessageVO mapToVO(ResultSet rs) throws SQLException {
        return new ChatMessageVO(
                rs.getLong("message_id"),
                rs.getLong("game_id"),
                rs.getLong("sender_id"),
                rs.getString("content"),
                rs.getTimestamp("sent_at").toLocalDateTime()
        );
    }
}