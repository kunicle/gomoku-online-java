# 五目並べオンライン / Omok Online

> リアルタイム対戦五目並べゲーム — Java Socket · MySQL · Python Flask AI  
> Real-time two-player Gomoku game with statistics, replay, and AI battle mode

![Java](https://img.shields.io/badge/Java-17+-orange)
![MySQL](https://img.shields.io/badge/MySQL-8.x-blue)
![Swing](https://img.shields.io/badge/UI-Java%20Swing-green)
![Python](https://img.shields.io/badge/AI-Python%20Flask-yellow)

---

## プロジェクト概要 / Project Overview

JavaとMySQLを活用して実装したリアルタイム対戦五目並べゲームです。  
単純なゲーム実装に留まらず、**プレイデータ（着手記録）をDBに蓄積**し、  
**ゲーム統計分析・リプレイ機能・AI対戦機能**を提供します。  
収集した着手データは、将来の推薦システムモデルの学習データとして活用可能な構造で設計されています。

Built with Java and MySQL, this real-time two-player Gomoku game goes beyond simple gameplay.  
It **collects play data (move records) into a database** and provides  
**game statistics, replay functionality, and AI battle mode**.  
Move data is structured for potential use as training data for a future recommendation system.

---

## 技術スタック / Tech Stack

| 区分 | 技術 | 用途 |
|------|------|------|
| 言語 | Java 17+ | Server / Client logic |
| UI | Java Swing | Desktop GUI client |
| DB | MySQL 8.x | Data persistence |
| 通信 | Java Socket (TCP) | Real-time battle & chat |
| AI | Python + Flask | Rule-based AI server |
| ビルド | IntelliJ IDEA | Development environment |

---

## 主な機能 / Key Features

### 会員管理 / User Management
- 新規登録 / ログイン (SHA-256 password hashing)
- ID・ニックネーム重複検証 / Duplicate validation

### マッチング / Matchmaking
- リアルタイムマッチング待機列 / Real-time matchmaking queue (FIFO)
- マッチング受諾・キャンセル / Accept / Cancel

### 五目並べ対戦 / Gomoku Game
- 19×19 リアルタイム対戦 / Real-time two-player battle (Java Swing direct rendering)
- 横・縦・斜め5目判定アルゴリズム / Win detection algorithm (4 directions)
- 着手制限時間30秒 / 30-second move timer (auto-lose on timeout)
- リアルタイム1:1チャット / Real-time in-game chat (persists after game end)
- 投了機能 / Resign feature

### AI対戦 / AI Battle
- Python Flask AIサーバーとHTTP連携 / Java-Python integration via REST API
- ルールベースエンジン / Rule-based engine (attack + defense scoring)
- AIサーバー自動起動 / Auto-start Flask server on launch
- 対戦記録DB保存・リプレイ対応 / Game records saved for replay

### ゲーム統計 / Game Statistics
- 勝率・平均着手時間・平均手数 / Win rate, avg think time, avg game length
- 勝率円グラフ可視化 / Pie chart visualization
- 全体ユーザーランキング / Overall ranking (by win count)

### リプレイ / Replay
- 過去対局の着手記録再生 / Step-by-step move playback
- 前へ・次へ・最初・自動再生 / Navigation controls + auto-play
- 勝利5目ハイライト表示 / Winning 5-stone highlight
- 投了ゲーム判別表示 / Resign game label

---

## パッケージ構成 / Package Structure

```
src
└── com
    └── omok
        ├── client          # UI Client (Java Swing)
        │   ├── MainFrame       - Main window, screen transitions (CardLayout)
        │   ├── LoginPanel      - Login / Registration screen
        │   ├── LobbyPanel      - Lobby: matchmaking, ranking, profile
        │   ├── GamePanel       - Game screen: board + chat + timer
        │   ├── BoardPanel      - Board rendering (Graphics2D)
        │   ├── StatsPanel      - Statistics screen with pie chart
        │   ├── ReplayPanel     - Replay screen with playback controls
        │   ├── SoloGamePanel   - AI battle screen
        │   ├── SoloStartDialog - Stone color selection dialog
        │   ├── NetworkClient   - Socket connection manager
        │   └── BotPlayer       - Flask AI server HTTP client
        ├── server          # Socket Server
        │   ├── OmokServer      - Server entry point
        │   ├── ClientHandler   - Per-client thread
        │   ├── MatchManager    - Matchmaking queue (Singleton)
        │   └── GameSession     - Game state, win detection, timer
        ├── db              # Data Access Layer
        │   ├── DBConnection    - JDBC connection (Singleton)
        │   ├── UserDAO         - users table CRUD
        │   ├── GameDAO         - games table CRUD
        │   ├── MoveDAO         - moves table CRUD
        │   ├── ChatMessageDAO  - chat_messages table CRUD
        │   └── StatsDAO        - Aggregation queries (JOIN / GROUP BY)
        ├── vo              # Value Objects
        │   ├── UserVO
        │   ├── GameVO
        │   ├── MoveVO
        │   ├── ChatMessageVO
        │   └── GameStatsVO
        ├── common
        │   └── MessageProtocol - Server-client message format constants
        └── util
            └── PasswordUtil    - SHA-256 hash utility
```

---

## DBテーブル構成 / Database Schema

```
users         — User info and win/lose/draw records
games         — Game metadata (players, result, win positions)
moves         — Every move record (coordinates, think time, color)
chat_messages — In-game chat logs
```

`moves` テーブルの `think_time_ms` は、将来の推薦システムにおける特徴量（Feature）として活用可能です。  
The `think_time_ms` field in the `moves` table is designed as a feature for future recommendation system training.

---

## 実行方法 / Getting Started

### 事前準備 / Prerequisites
- Java 17+
- MySQL 8.x
- Python 3.x + Flask (`pip install flask`)
- mysql-connector-j-8.3.0.jar (add to project libraries)

### 1. データベース設定 / Database Setup

```sql
CREATE DATABASE omok_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
```

Run the table creation SQL from `sql/init.sql`.

### 2. DB接続情報の変更 / Configure DB Connection

Edit `src/com/omok/db/DBConnection.java`:

```java
private static final String PASSWORD = "your_mysql_password";
```

### 3. サーバー起動 / Start Server

```
Run: com.omok.server.OmokServer
→ "Server ready. Waiting for clients..." 
```

### 4. クライアント起動 / Start Client

```
Run: com.omok.client.MainFrame
→ Flask AI server starts automatically
→ Login screen appears
```

> 同一PC上で2人対戦する場合、IntelliJ の **Allow multiple instances** を有効にして MainFrame を2回起動してください。  
> For local 2-player, enable **Allow multiple instances** in IntelliJ and run MainFrame twice.

---

## 他のPCから接続する方法 / Connecting from Another PC

1. Open TCP port **9999** in the server PC's firewall
2. Edit `NetworkClient.java`:

```java
private static final String HOST = "192.168.x.x";  // Server PC IP
```

---

## AIアーキテクチャ / AI Architecture

```
Java (BotPlayer)
    ↓ HTTP POST /predict {"board": [...], "ai_color": 2}
Python Flask server (omok_ai/app.py)
    ↓ Rule-based engine (evaluate_position + find_best_move)
    ↑ {"row": 9, "col": 10}
Java (BotPlayer) → BoardPanel rendering
```

FlaskサーバーをREST APIとして分離することで、将来的にAIモデルを差し替え可能な構造にしています。  
The Flask server is separated as a REST API, making it easy to swap in a trained AI model in the future.

---

## 開発上の工夫 / Design Highlights

**1. スレッドセーフな待機列**  
`ConcurrentLinkedQueue` を使用し、複数のクライアントが同時に待機列にアクセスしても安全な構造を実現。

**2. 着手データの収集設計**  
`moves` テーブルに着手座標・所要時間・石の色を記録することで、統計分析とリプレイ機能を実現。この構造は推薦システムのユーザー行動ログ収集と同じ概念です。

**3. Java-Python連携**  
HTTPによる言語間通信により、JavaクライアントからPython AIエンジンを呼び出す仕組みを実装。AIロジックの差し替えが容易な拡張性を確保。

**4. シングルトンパターン**  
`DBConnection` と `MatchManager` にシングルトンパターンを適用し、リソースの効率的な管理を実現。

---

## 未実装事項 / Known Limitations

- 三三・四四の禁手ルール / Forbidden moves (double-three, double-four)
- 長目（6目以上無効）ルール / Overline rule (6+ in a row invalid)

