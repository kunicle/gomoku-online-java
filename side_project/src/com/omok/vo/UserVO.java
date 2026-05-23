package com.omok.vo;

import java.time.LocalDateTime;

/**
 * Value Object mapping to the users table.
 * Immutable by design - all fields are final with no setters.
 */
public class UserVO {

    private final long userId;
    private final String username;
    private final String passwordHash;
    private final String nickname;
    private final int winCount;
    private final int loseCount;
    private final int drawCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastLoginAt;  // Nullable

    public UserVO(long userId, String username, String passwordHash, String nickname,
                  int winCount, int loseCount, int drawCount,
                  LocalDateTime createdAt, LocalDateTime lastLoginAt) {
        this.userId       = userId;
        this.username     = username;
        this.passwordHash = passwordHash;
        this.nickname     = nickname;
        this.winCount     = winCount;
        this.loseCount    = loseCount;
        this.drawCount    = drawCount;
        this.createdAt    = createdAt;
        this.lastLoginAt  = lastLoginAt;
    }

    public long getUserId()               { return userId; }
    public String getUsername()           { return username; }
    public String getPasswordHash()       { return passwordHash; }
    public String getNickname()           { return nickname; }
    public int getWinCount()              { return winCount; }
    public int getLoseCount()             { return loseCount; }
    public int getDrawCount()             { return drawCount; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
}