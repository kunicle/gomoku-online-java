package com.omok.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Renders the Gomoku board and handles stone placement via mouse clicks.
 * Overrides paintComponent() to draw directly with Graphics2D.
 */
public class BoardPanel extends JPanel {

    // Board configuration
    private static final int BOARD_SIZE  = 19;      // 19x19 grid
    private static final int CELL_SIZE   = 34;      // Cell size in pixels
    private static final int PADDING     = 32;      // Board margin
    private static final int STONE_RATIO = 13;      // Stone radius

    // Board state: 0=empty, 1=black, 2=white
    private final int[][] board = new int[BOARD_SIZE][BOARD_SIZE];

    // Last placed stone position (for highlight marker)
    private int lastRow = -1, lastCol = -1;

    // Winning 5 stones for highlight display
    private int[][] winStones = null;

    // Whether it's the player's turn to place a stone
    private boolean myTurn = false;

    // Click callback - injected by GamePanel to send move to server
    public interface StoneClickListener {
        void onStoneClick(int row, int col);
    }
    private StoneClickListener clickListener;

    public BoardPanel() {
        int panelSize = CELL_SIZE * (BOARD_SIZE - 1) + PADDING * 2;
        setPreferredSize(new Dimension(panelSize, panelSize));
        setMinimumSize(new Dimension(panelSize, panelSize));
        setMaximumSize(new Dimension(panelSize, panelSize));
        setBackground(new Color(220, 179, 92));  // Wood color

        // Mouse click → convert pixel coordinates to intersection coordinates
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!myTurn || clickListener == null) return;

                // Convert click pixel → nearest intersection (row, col)
                int col = Math.round((float)(e.getX() - PADDING) / CELL_SIZE);
                int row = Math.round((float)(e.getY() - PADDING) / CELL_SIZE);

                // Bounds check
                if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) return;

                // Ignore if cell is already occupied
                if (board[row][col] != 0) return;

                clickListener.onStoneClick(row, col);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Anti-aliasing for smooth stone and line rendering
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrid(g2);
        drawDots(g2);
        drawStones(g2);
        drawWinHighlight(g2);
    }

    // Draw grid lines
    private void drawGrid(Graphics2D g2) {
        g2.setColor(new Color(139, 90, 20));
        g2.setStroke(new BasicStroke(0.8f));

        for (int i = 0; i < BOARD_SIZE; i++) {
            int x = PADDING + i * CELL_SIZE;
            int y = PADDING + i * CELL_SIZE;
            int end = PADDING + (BOARD_SIZE - 1) * CELL_SIZE;

            g2.drawLine(x, PADDING, x, end);  // Vertical lines
            g2.drawLine(PADDING, y, end, y);  // Horizontal lines
        }
    }

    // Draw star points (hoshi) at standard positions
    private void drawDots(Graphics2D g2) {
        g2.setColor(new Color(139, 90, 20));
        int[][] dots = {{3,3},{3,9},{3,15},{9,3},{9,9},{9,15},{15,3},{15,9},{15,15}};
        for (int[] d : dots) {
            int x = PADDING + d[1] * CELL_SIZE;
            int y = PADDING + d[0] * CELL_SIZE;
            g2.fillOval(x - 4, y - 4, 8, 8);
        }
    }

    // Draw all stones on the board
    private void drawStones(Graphics2D g2) {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (board[r][c] == 0) continue;

                int x = PADDING + c * CELL_SIZE;
                int y = PADDING + r * CELL_SIZE;
                boolean isBlack = board[r][c] == 1;

                // Fill stone
                g2.setColor(isBlack ? new Color(30, 30, 30) : new Color(240, 240, 230));
                g2.fillOval(x - STONE_RATIO, y - STONE_RATIO, STONE_RATIO * 2, STONE_RATIO * 2);

                // Stone border
                g2.setColor(isBlack ? new Color(60, 60, 60) : new Color(180, 180, 170));
                g2.setStroke(new BasicStroke(1f));
                g2.drawOval(x - STONE_RATIO, y - STONE_RATIO, STONE_RATIO * 2, STONE_RATIO * 2);

                // Last move marker (red dot)
                if (r == lastRow && c == lastCol) {
                    g2.setColor(new Color(220, 50, 50));
                    g2.fillOval(x - 4, y - 4, 8, 8);
                }
            }
        }
    }

    // Highlight winning 5 stones with red circle
    private void drawWinHighlight(Graphics2D g2) {
        if (winStones == null) return;
        g2.setColor(new Color(255, 50, 50, 220));
        g2.setStroke(new BasicStroke(4f));
        for (int[] s : winStones) {
            int x = PADDING + s[1] * CELL_SIZE;
            int y = PADDING + s[0] * CELL_SIZE;
            g2.drawOval(x - STONE_RATIO - 2, y - STONE_RATIO - 2,
                    STONE_RATIO * 2 + 4, STONE_RATIO * 2 + 4);
        }
    }

    // Place a stone on the board (called when server confirms move)
    public void placeStone(int row, int col, int color) {
        board[row][col] = color;
        lastRow = row;
        lastCol = col;
        repaint();
    }

    // Highlight winning stones at game end
    public void highlightWin(int[][] stones) {
        this.winStones = stones;
        repaint();
    }

    // Reset board for new game
    public void reset() {
        for (int[] row : board) java.util.Arrays.fill(row, 0);
        lastRow = -1;
        lastCol = -1;
        winStones = null;
        repaint();
    }

    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
        // Change cursor to hand when it's the player's turn
        setCursor(myTurn
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());
    }

    public void setClickListener(StoneClickListener listener) {
        this.clickListener = listener;
    }
}