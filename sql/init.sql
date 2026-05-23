-- =====================================================
-- Omok Online - Database Initialization Script
-- Run this script to set up the database from scratch
-- =====================================================

CREATE DATABASE IF NOT EXISTS omok_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE omok_db;

-- ── users ──────────────────────────────────────────
CREATE TABLE users (
    user_id       BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname      VARCHAR(50)  NOT NULL,
    win_count     INT          NOT NULL DEFAULT 0,
    lose_count    INT          NOT NULL DEFAULT 0,
    draw_count    INT          NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT NOW(),
    last_login_at DATETIME     NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_username (username),
    UNIQUE KEY uq_nickname (nickname)
);

-- ── games ──────────────────────────────────────────
-- player2_id is NULL for solo (AI) games
CREATE TABLE games (
    game_id       BIGINT       NOT NULL AUTO_INCREMENT,
    player1_id    BIGINT       NOT NULL,
    player2_id    BIGINT       NULL,
    winner_id     BIGINT       NULL,
    result        ENUM('BLACK_WIN','WHITE_WIN','DRAW') NULL,
    total_moves   INT          NULL,
    win_positions VARCHAR(200) NULL,
    started_at    DATETIME     NOT NULL,
    ended_at      DATETIME     NULL,
    PRIMARY KEY (game_id),
    FOREIGN KEY (player1_id) REFERENCES users(user_id),
    FOREIGN KEY (player2_id) REFERENCES users(user_id),
    FOREIGN KEY (winner_id)  REFERENCES users(user_id)
);

-- ── moves ──────────────────────────────────────────
-- player_id is NULL for AI moves
-- think_time_ms: feature data for recommendation system training
CREATE TABLE moves (
    move_id       BIGINT   NOT NULL AUTO_INCREMENT,
    game_id       BIGINT   NOT NULL,
    player_id     BIGINT   NULL,
    move_number   INT      NOT NULL,
    row_pos       TINYINT  NOT NULL,
    col_pos       TINYINT  NOT NULL,
    stone_color   ENUM('BLACK','WHITE') NOT NULL,
    think_time_ms INT      NULL,
    placed_at     DATETIME NOT NULL,
    PRIMARY KEY (move_id),
    FOREIGN KEY (game_id)   REFERENCES games(game_id),
    FOREIGN KEY (player_id) REFERENCES users(user_id)
);

-- ── chat_messages ───────────────────────────────────
CREATE TABLE chat_messages (
    message_id BIGINT       NOT NULL AUTO_INCREMENT,
    game_id    BIGINT       NOT NULL,
    sender_id  BIGINT       NOT NULL,
    content    VARCHAR(500) NOT NULL,
    sent_at    DATETIME     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (message_id),
    FOREIGN KEY (game_id)   REFERENCES games(game_id),
    FOREIGN KEY (sender_id) REFERENCES users(user_id)
);
