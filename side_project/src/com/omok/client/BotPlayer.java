package com.omok.client;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Communicates with the Python Flask AI server via HTTP.
 * Sends the current board state as JSON and receives the AI's move coordinates.
 */
public class BotPlayer {

    private static final String FLASK_URL = "http://127.0.0.1:5001/predict";
    private final int aiColor;  // 1=black, 2=white

    public BotPlayer(int aiColor) {
        this.aiColor = aiColor;
    }

    /**
     * Sends board state to Flask server and returns AI move coordinates.
     * @param board Current board state (0=empty, 1=black, 2=white)
     * @return [row, col] coordinates, or null on failure
     */
    public int[] requestMove(int[][] board) {
        try {
            String requestJson = buildJson(board);

            URL url = new URL(FLASK_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);  // 3 second connection timeout
            conn.setReadTimeout(5000);     // 5 second read timeout

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestJson.getBytes("UTF-8"));
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }
            }

            String responseJson = response.toString();
            System.out.println("AI response: " + responseJson);
            return parseResponse(responseJson);

        } catch (Exception e) {
            System.out.println("AI server request failed: " + e.getMessage());
            return null;
        }
    }

    // Convert board array to JSON string
    // Example: [[0,1,...], ...] → {"board":[[0,1,...],...], "ai_color":2}
    private String buildJson(int[][] board) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"board\":[");
        for (int r = 0; r < board.length; r++) {
            sb.append("[");
            for (int c = 0; c < board[r].length; c++) {
                sb.append(board[r][c]);
                if (c < board[r].length - 1) sb.append(",");
            }
            sb.append("]");
            if (r < board.length - 1) sb.append(",");
        }
        sb.append("],\"ai_color\":").append(aiColor).append("}");
        return sb.toString();
    }

    // Parse JSON response: {"row": 9, "col": 10} → [9, 10]
    // Implemented without external libraries
    private int[] parseResponse(String json) {
        int row = extractInt(json, "row");
        int col = extractInt(json, "col");
        return new int[]{row, col};
    }

    private int extractInt(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1) return -1;
        int start = idx + search.length();

        // Skip whitespace
        while (start < json.length() && json.charAt(start) == ' ') start++;

        int end = start;
        while (end < json.length() &&
                (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        if (start == end) return -1;
        return Integer.parseInt(json.substring(start, end).trim());
    }

    public int getAiColor() { return aiColor; }
}