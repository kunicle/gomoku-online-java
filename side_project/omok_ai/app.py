from flask import Flask, request, jsonify
import random

app = Flask(__name__)

BOARD_SIZE = 19

def evaluate_position(board, row, col, color):
    """
    Calculate the score for placing a stone at (row, col).
    Searches 4 directions and scores based on consecutive stone count and open ends.
    Higher score = more favorable position.
    """
    directions = [(0, 1), (1, 0), (1, 1), (1, -1)]
    score = 0

    for dr, dc in directions:
        count = 1
        open_ends = 0

        # Forward direction
        r, c = row + dr, col + dc
        while 0 <= r < BOARD_SIZE and 0 <= c < BOARD_SIZE and board[r][c] == color:
            count += 1
            r += dr
            c += dc
        if 0 <= r < BOARD_SIZE and 0 <= c < BOARD_SIZE and board[r][c] == 0:
            open_ends += 1

        # Backward direction
        r, c = row - dr, col - dc
        while 0 <= r < BOARD_SIZE and 0 <= c < BOARD_SIZE and board[r][c] == color:
            count += 1
            r -= dr
            c -= dc
        if 0 <= r < BOARD_SIZE and 0 <= c < BOARD_SIZE and board[r][c] == 0:
            open_ends += 1

        # Score by consecutive count and open ends
        if count >= 5:
            score += 100000   # Win condition
        elif count == 4:
            score += 10000 if open_ends == 2 else 1000   # Open 4 vs blocked 4
        elif count == 3:
            score += 1000 if open_ends == 2 else 100     # Open 3 vs blocked 3
        elif count == 2:
            score += 100 if open_ends == 2 else 10       # Open 2 vs blocked 2

    return score


def find_best_move(board, ai_color):
    """
    Find the best move using a greedy strategy.
    Evaluates each empty cell by combining attack score and defense score.
    Only considers cells within 2 steps of existing stones for efficiency.

    Score formula: attack_score + defense_score * 0.8
    Attack is prioritized over defense.
    """
    player_color = 1 if ai_color == 2 else 2
    best_score = -1
    best_moves = []

    # Place in center if board is empty
    if all(board[r][c] == 0 for r in range(BOARD_SIZE) for c in range(BOARD_SIZE)):
        return [BOARD_SIZE // 2, BOARD_SIZE // 2]

    for row in range(BOARD_SIZE):
        for col in range(BOARD_SIZE):
            if board[row][col] != 0:
                continue

            # Skip cells with no neighbors within 2 steps (efficiency optimization)
            has_neighbor = False
            for dr in range(-2, 3):
                for dc in range(-2, 3):
                    r, c = row + dr, col + dc
                    if 0 <= r < BOARD_SIZE and 0 <= c < BOARD_SIZE and board[r][c] != 0:
                        has_neighbor = True
                        break
                if has_neighbor:
                    break
            if not has_neighbor:
                continue

            # Calculate attack and defense scores
            attack_score  = evaluate_position(board, row, col, ai_color)
            defense_score = evaluate_position(board, row, col, player_color)

            # Weighted total: prioritize attack over defense
            total_score = attack_score + int(defense_score * 0.8)

            if total_score > best_score:
                best_score = total_score
                best_moves = [[row, col]]
            elif total_score == best_score:
                best_moves.append([row, col])

    # Random tiebreak among equally scored moves
    if best_moves:
        return random.choice(best_moves)

    # Fallback: random empty cell
    empty = [[r, c] for r in range(BOARD_SIZE)
             for c in range(BOARD_SIZE) if board[r][c] == 0]
    return random.choice(empty) if empty else None


@app.route('/predict', methods=['POST'])
def predict():
    """
    Receive board state from Java client and return AI move coordinates.

    Request format:
    {
        "board": [[0,0,...], ...],  // 19x19 array (0=empty, 1=black, 2=white)
        "ai_color": 2               // AI stone color (1=black, 2=white)
    }

    Response format:
    {
        "row": 9,
        "col": 9
    }
    """
    data = request.json
    board = data['board']
    ai_color = data['ai_color']

    move = find_best_move(board, ai_color)

    if move is None:
        return jsonify({'error': 'No available moves'}), 400

    print(f"AI move: row={move[0]}, col={move[1]}")
    return jsonify({'row': move[0], 'col': move[1]})


@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint - used by Java to verify server is ready."""
    return jsonify({'status': 'ok'})


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)