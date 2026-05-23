package com.omok.db;

import com.omok.util.PasswordUtil;
import com.omok.vo.UserVO;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // Retrieves Connection from DBConnection and stores it as a field
    private final Connection conn;

    public UserDAO() throws SQLException {
        this.conn = DBConnection.getInstance().getConnection();
    }

    // Register: Insert a new user into the users table
    // Password is hashed before storage
    // Throws SQLException if username/nickname is duplicated
    public void insert(String username, String password, String nickname) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, nickname, created_at) " +
                "VALUES (?, ?, ?, ?)";

        // PreparedStatement: Binds values to ? placeholders to prevent SQL Injection
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));
            ps.setString(3, nickname);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    // Login: Find user by username and verify password
    // Returns UserVO on success, null on failure
    public UserVO login(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");

                    // Hash the input password and compare with stored hash
                    if (PasswordUtil.verify(password, storedHash)) {
                        updateLastLogin(rs.getLong("user_id"));
                        return mapToVO(rs);
                    }
                }
            }
        }
        // Returns null if username not found or password is incorrect
        return null;
    }

    // Check username duplication (used during registration)
    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    // Check nickname duplication (used during registration)
    public boolean existsByNickname(String nickname) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE nickname = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    // Find user by user_id (used to refresh user info after returning to lobby)
    public UserVO findByUserId(long userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapToVO(rs);
            }
        }
        return null;
    }

    // Update win/lose/draw count after game result
    public void updateRecord(long userId, String result) throws SQLException {
        String col;
        switch (result) {
            case "WIN":  col = "win_count";  break;
            case "LOSE": col = "lose_count"; break;
            default:     col = "draw_count"; break;
        }
        String sql = "UPDATE users SET " + col + " = " + col + " + 1 WHERE user_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    // Retrieve top users ranked by win count (used for ranking display)
    public List<UserVO> getRanking(int limit) throws SQLException {
        String sql = "SELECT *, " +
                "(win_count / NULLIF(win_count + lose_count + draw_count, 0)) AS win_rate " +
                "FROM users ORDER BY win_count DESC, win_rate DESC LIMIT ?";
        List<UserVO> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapToVO(rs));
            }
        }
        return list;
    }

    // Update last login timestamp on successful login
    private void updateLastLogin(long userId) throws SQLException {
        String sql = "UPDATE users SET last_login_at = ? WHERE user_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    // Maps a ResultSet row to a UserVO object
    // DB column names → Java field names
    private UserVO mapToVO(ResultSet rs) throws SQLException {
        return new UserVO(
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("nickname"),
                rs.getInt("win_count"),
                rs.getInt("lose_count"),
                rs.getInt("draw_count"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("last_login_at") != null
                        ? rs.getTimestamp("last_login_at").toLocalDateTime() : null
        );
    }
}