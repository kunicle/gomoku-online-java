package com.omok.vo;

import java.time.LocalDateTime;

/**
 * Value Object mapping to the chat_messages table.
 * Stores in-game chat messages linked to a specific game session.
 */
public class ChatMessageVO {

    private final long messageId;
    private final long gameId;
    private final long senderId;
    private final String content;
    private final LocalDateTime sentAt;

    public ChatMessageVO(long messageId, long gameId, long senderId,
                         String content, LocalDateTime sentAt) {
        this.messageId = messageId;
        this.gameId    = gameId;
        this.senderId  = senderId;
        this.content   = content;
        this.sentAt    = sentAt;
    }

    public long getMessageId()         { return messageId; }
    public long getGameId()            { return gameId; }
    public long getSenderId()          { return senderId; }
    public String getContent()         { return content; }
    public LocalDateTime getSentAt()   { return sentAt; }
}