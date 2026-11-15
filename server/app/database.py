import sqlite3
from pathlib import Path

DB_PATH = Path(__file__).resolve().parent.parent / "data" / "kora_scanner.db"


def get_connection() -> sqlite3.Connection:
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(DB_PATH, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn


def init_db(conn: sqlite3.Connection) -> None:
    conn.executescript(
        """
        CREATE TABLE IF NOT EXISTS concerts (
            id TEXT PRIMARY KEY,
            title TEXT NOT NULL,
            venue TEXT,
            date_time TEXT,
            revision INTEGER NOT NULL DEFAULT 1
        );

        CREATE TABLE IF NOT EXISTS places (
            concert_id TEXT NOT NULL,
            place_id TEXT NOT NULL,
            row_label TEXT,
            seat_label TEXT,
            sector TEXT,
            is_pushkin INTEGER NOT NULL DEFAULT 0,
            is_redeemed INTEGER NOT NULL DEFAULT 0,
            redeemed_at_ms INTEGER,
            PRIMARY KEY (concert_id, place_id),
            FOREIGN KEY (concert_id) REFERENCES concerts(id) ON DELETE CASCADE
        );

        CREATE TABLE IF NOT EXISTS barcodes (
            barcode TEXT PRIMARY KEY,
            concert_id TEXT NOT NULL,
            place_id TEXT NOT NULL,
            source TEXT NOT NULL DEFAULT 'api',
            FOREIGN KEY (concert_id, place_id) REFERENCES places(concert_id, place_id) ON DELETE CASCADE
        );

        CREATE TABLE IF NOT EXISTS redemption_log (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            concert_id TEXT NOT NULL,
            place_id TEXT NOT NULL,
            barcode TEXT NOT NULL,
            is_pushkin INTEGER NOT NULL,
            scanned_at_ms INTEGER NOT NULL
        );
        """
    )
    conn.commit()