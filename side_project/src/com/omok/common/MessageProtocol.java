package com.omok.common;

// Defines message type constants for server-client communication
// All messages follow the format: "TYPE|DATA"
// Example: "CHAT|Hello", "MOVE|3|4"
public class MessageProtocol {

    private MessageProtocol() {}

    // ── Authentication ──
    public static final String LOGIN      = "LOGIN";      // LOGIN|username|password
    public static final String REGISTER   = "REGISTER";   // REGISTER|username|password|nickname
    public static final String LOGIN_OK   = "LOGIN_OK";   // LOGIN_OK|nickname
    public static final String LOGIN_FAIL = "LOGIN_FAIL"; // LOGIN_FAIL|reason

    // ── Matching ──
    public static final String MATCH_REQUEST = "MATCH_REQUEST"; // Request matchmaking
    public static final String MATCH_CANCEL  = "MATCH_CANCEL";  // Cancel matchmaking
    public static final String MATCH_FOUND   = "MATCH_FOUND";   // MATCH_FOUND|opponentNick|myColor(BLACK/WHITE)

    // ── Game ──
    public static final String MOVE      = "MOVE";       // MOVE|row|col
    public static final String MOVE_OK   = "MOVE_OK";    // MOVE_OK|row|col|color
    public static final String GAME_OVER = "GAME_OVER";  // GAME_OVER|result(WIN/LOSE/DRAW)
    public static final String TIMEOUT   = "TIMEOUT";    // Lose by timeout
    public static final String RESIGN    = "RESIGN";     // Resign (keep connection)

    // ── Chat ──
    public static final String CHAT = "CHAT"; // CHAT|nickname|message

    // ── Misc ──
    public static final String PING       = "PING";        // Connection check
    public static final String PONG       = "PONG";        // PING response
    public static final String ERROR      = "ERROR";       // ERROR|message
    public static final String DISCONNECT = "DISCONNECT";  // Disconnect

    // Delimiter
    public static final String DELIMITER = "|";

    // Message builder helper
    // Example: build(MOVE, "3", "4") → "MOVE|3|4"
    public static String build(String type, String... args) {
        if (args.length == 0) return type;
        return type + DELIMITER + String.join(DELIMITER, args);
    }

    // Message parser helper
    // Example: "MOVE|3|4" → ["MOVE", "3", "4"]
    public static String[] parse(String message) {
        return message.split("\\" + DELIMITER);
    }
}