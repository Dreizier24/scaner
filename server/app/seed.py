import sqlite3
import time


def seed_if_empty(conn: sqlite3.Connection) -> None:
    row = conn.execute("SELECT COUNT(*) AS c FROM concerts").fetchone()
    if row["c"] > 0:
        return

    concerts = [
        (
            "c-symphony-2026-11-05",
            "Государственный симфонический оркестр — «Волшебный луч»",
            "г. Майкоп, ул. Пионерская, 300",
            "05.11.2026 18:30",
            1,
        ),
        (
            "c-islamey-2026-11-12",
            "Ансамбль «Исламей» — Сила песни – энергия танца",
            "г. Майкоп, ул. Краснооктябрьская, 61",
            "12.11.2026 11:00",
            1,
        ),
        (
            "c-theatre-2026-11-28",
            "«Здравствуйте, я ваша тётя!»",
            "г. Майкоп, ул. Пионерская, 300",
            "28.11.2026 19:00",
            1,
        ),
    ]

    conn.executemany(
        "INSERT INTO concerts (id, title, venue, date_time, revision) VALUES (?, ?, ?, ?, ?)",
        concerts,
    )

    # Несколько билетов в первом концерте (для тестового сканирования)
    symphony_places = [
        ("p-001", "3", "12", "Партер", 0, "KORA-C1-3-12"),
        ("p-002", "3", "13", "Партер", 0, "KORA-C1-3-13"),
        ("p-003", "5", "1", "Партер", 0, "KORA-C1-5-01"),
        ("p-004", "7", "8", "Бельэтаж", 1, "PUSHKIN-C1-7-08"),
        ("p-005", "7", "9", "Бельэтаж", 1, "PUSHKIN-C1-7-09"),
        ("p-006", "1", "1", "VIP", 0, "4601234567890"),
    ]

    insert_place(symphony_places, "c-symphony-2026-11-05", conn)

    # По 2 места в остальных концертах
    islamey = [
        ("p-101", "2", "4", "Партер", 0, "KORA-C2-2-04"),
        ("p-102", "2", "5", "Партер", 0, "KORA-C2-2-05"),
    ]
    insert_place(islamey, "c-islamey-2026-11-12", conn)

    theatre = [
        ("p-201", "10", "15", "Амфитеатр", 0, "KORA-C3-10-15"),
        ("p-202", "10", "16", "Амфитеатр", 0, "KORA-C3-10-16"),
    ]
    insert_place(theatre, "c-theatre-2026-11-28", conn)

    conn.commit()


def insert_place(
    rows: list[tuple],
    concert_id: str,
    conn: sqlite3.Connection,
) -> None:
    for place_id, row, seat, sector, is_pushkin, barcode in rows:
        conn.execute(
            """
            INSERT INTO places (concert_id, place_id, row_label, seat_label, sector, is_pushkin, is_redeemed)
            VALUES (?, ?, ?, ?, ?, ?, 0)
            """,
            (concert_id, place_id, row, seat, sector, is_pushkin),
        )
        conn.execute(
            """
            INSERT INTO barcodes (barcode, concert_id, place_id, source)
            VALUES (?, ?, ?, 'seed')
            """,
            (barcode, concert_id, place_id),
        )


def ensure_loadtest_concert(conn: sqlite3.Connection, ticket_count: int = 2000) -> None:
    """Демо-событие с большим числом билетов (для проверки sync/сканера)."""
    cid = "c-loadtest-2000"
    row = conn.execute(
        "SELECT COUNT(*) AS c FROM places WHERE concert_id = ?",
        (cid,),
    ).fetchone()
    if int(row["c"]) >= ticket_count:
        return

    conn.execute("DELETE FROM barcodes WHERE concert_id = ?", (cid,))
    conn.execute("DELETE FROM places WHERE concert_id = ?", (cid,))
    conn.execute("DELETE FROM concerts WHERE id = ?", (cid,))

    conn.execute(
        """
        INSERT INTO concerts (id, title, venue, date_time, revision)
        VALUES (?, ?, ?, ?, ?)
        """,
        (
            cid,
            "Нагрузочный тест — 2000 мест",
            "г. Майкоп, ДК",
            "01.12.2026 19:00",
            1,
        ),
    )

    places: list[tuple] = []
    codes: list[tuple] = []
    for i in range(ticket_count):
        pid = f"p-load-{i:05d}"
        row_n = str((i // 50) + 1)
        seat_n = str((i % 50) + 1)
        barcode = f"LOAD2000-{i:05d}"
        places.append((cid, pid, row_n, seat_n, "Партер", 0))
        codes.append((barcode, cid, pid, "seed"))

    conn.executemany(
        """
        INSERT INTO places (concert_id, place_id, row_label, seat_label, sector, is_pushkin, is_redeemed)
        VALUES (?, ?, ?, ?, ?, ?, 0)
        """,
        places,
    )
    conn.executemany(
        """
        INSERT INTO barcodes (barcode, concert_id, place_id, source)
        VALUES (?, ?, ?, ?)
        """,
        codes,
    )
    bump_revision(conn, cid)


def bump_revision(conn: sqlite3.Connection, concert_id: str) -> int:
    conn.execute(
        "UPDATE concerts SET revision = revision + 1 WHERE id = ?",
        (concert_id,),
    )
    row = conn.execute(
        "SELECT revision FROM concerts WHERE id = ?",
        (concert_id,),
    ).fetchone()
    conn.commit()
    return int(row["revision"])