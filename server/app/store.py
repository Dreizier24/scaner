import sqlite3
import time
import uuid

from app.seed import bump_revision
from app.schemas import (
    ConcertDto,
    PlaceManifestDto,
    ValidateTicketResponse,
)


def list_concerts(conn: sqlite3.Connection) -> list[ConcertDto]:
    rows = conn.execute(
        "SELECT id, title, venue, date_time FROM concerts ORDER BY date_time"
    ).fetchall()
    return [
        ConcertDto(
            id=r["id"],
            title=r["title"],
            venue=r["venue"],
            dateTime=r["date_time"],
        )
        for r in rows
    ]


def get_concert_revision(conn: sqlite3.Connection, concert_id: str) -> int | None:
    row = conn.execute(
        "SELECT revision FROM concerts WHERE id = ?",
        (concert_id,),
    ).fetchone()
    return int(row["revision"]) if row else None


def build_manifest(
    conn: sqlite3.Connection,
    concert_id: str,
    since: int | None,
) -> tuple[bool, int, list[PlaceManifestDto]]:
    rev = get_concert_revision(conn, concert_id)
    if rev is None:
        return False, 0, []

    if since is not None and since >= rev:
        return True, rev, []

    places_rows = conn.execute(
        """
        SELECT place_id, row_label, seat_label, sector, is_pushkin, is_redeemed
        FROM places WHERE concert_id = ? ORDER BY row_label, seat_label
        """,
        (concert_id,),
    ).fetchall()

    places: list[PlaceManifestDto] = []
    for p in places_rows:
        codes = conn.execute(
            "SELECT barcode FROM barcodes WHERE concert_id = ? AND place_id = ?",
            (concert_id, p["place_id"]),
        ).fetchall()
        places.append(
            PlaceManifestDto(
                placeId=p["place_id"],
                row=p["row_label"],
                seat=p["seat_label"],
                sector=p["sector"],
                barcodes=[c["barcode"] for c in codes],
                isPushkin=bool(p["is_pushkin"]),
                isRedeemed=bool(p["is_redeemed"]),
            )
        )

    return False, rev, places


def apply_redemptions(
    conn: sqlite3.Connection,
    concert_id: str,
    items: list,
) -> int:
    accepted = 0
    now = int(time.time() * 1000)

    for item in items:
        place = conn.execute(
            """
            SELECT place_id, is_redeemed, is_pushkin FROM places
            WHERE concert_id = ? AND place_id = ?
            """,
            (concert_id, item.placeId),
        ).fetchone()
        if not place:
            continue

        if place["is_redeemed"]:
            continue

        conn.execute(
            """
            UPDATE places
            SET is_redeemed = 1, redeemed_at_ms = ?
            WHERE concert_id = ? AND place_id = ?
            """,
            (item.scannedAtEpochMs or now, concert_id, item.placeId),
        )
        conn.execute(
            """
            INSERT INTO redemption_log (concert_id, place_id, barcode, is_pushkin, scanned_at_ms)
            VALUES (?, ?, ?, ?, ?)
            """,
            (
                concert_id,
                item.placeId,
                item.barcode,
                1 if item.isPushkin else 0,
                item.scannedAtEpochMs,
            ),
        )
        accepted += 1

    if accepted > 0:
        bump_revision(conn, concert_id)
    else:
        conn.commit()

    return accepted


def import_barcodes(conn: sqlite3.Connection, concert_id: str, rows: list) -> int:
    if get_concert_revision(conn, concert_id) is None:
        return 0

    for idx, row in enumerate(rows):
        place_id = row.placeId or f"ext-{uuid.uuid4().hex[:8]}"
        conn.execute(
            """
            INSERT INTO places (concert_id, place_id, row_label, seat_label, sector, is_pushkin, is_redeemed)
            VALUES (?, ?, ?, ?, ?, ?, 0)
            ON CONFLICT(concert_id, place_id) DO UPDATE SET
                row_label = excluded.row_label,
                seat_label = excluded.seat_label,
                sector = excluded.sector,
                is_pushkin = excluded.is_pushkin
            """,
            (
                concert_id,
                place_id,
                row.row,
                row.seat,
                row.sector,
                1 if row.isPushkin else 0,
            ),
        )
        conn.execute(
            """
            INSERT INTO barcodes (barcode, concert_id, place_id, source)
            VALUES (?, ?, ?, 'import')
            ON CONFLICT(barcode) DO UPDATE SET
                concert_id = excluded.concert_id,
                place_id = excluded.place_id,
                source = 'import'
            """,
            (row.barcode.strip(), concert_id, place_id),
        )

    bump_revision(conn, concert_id)
    return len(rows)


def redeem_pushkin(
    conn: sqlite3.Connection,
    concert_id: str,
    place_id: str,
    barcode: str,
    scanned_at_ms: int,
) -> tuple[str, str | None]:
    place = conn.execute(
        """
        SELECT is_pushkin, is_redeemed FROM places
        WHERE concert_id = ? AND place_id = ?
        """,
        (concert_id, place_id),
    ).fetchone()
    if not place:
        return "not_found", "Место не найдено"
    if not place["is_pushkin"]:
        return "unknown", "Билет не по Пушкинской карте"
    if place["is_redeemed"]:
        return "already_used", "Билет уже погашен"

    conn.execute(
        """
        UPDATE places SET is_redeemed = 1, redeemed_at_ms = ?
        WHERE concert_id = ? AND place_id = ?
        """,
        (scanned_at_ms, concert_id, place_id),
    )
    conn.execute(
        """
        INSERT INTO redemption_log (concert_id, place_id, barcode, is_pushkin, scanned_at_ms)
        VALUES (?, ?, ?, 1, ?)
        """,
        (concert_id, place_id, barcode, scanned_at_ms),
    )
    bump_revision(conn, concert_id)
    return "ok", "Пушкинская карта: вход разрешён"


def validate_ticket(conn: sqlite3.Connection, ticket_id: str | None, raw: str) -> ValidateTicketResponse:
    code = raw.strip()
    row = conn.execute(
        """
        SELECT b.barcode, p.place_id, p.row_label, p.seat_label, p.sector,
               p.is_redeemed, p.is_pushkin, c.id AS concert_id, c.title, c.venue, c.date_time
        FROM barcodes b
        JOIN places p ON p.concert_id = b.concert_id AND p.place_id = b.place_id
        JOIN concerts c ON c.id = b.concert_id
        WHERE b.barcode = ? OR (? IS NOT NULL AND p.place_id = ?)
        LIMIT 1
        """,
        (code, ticket_id, ticket_id),
    ).fetchone()

    if not row:
        return ValidateTicketResponse(status="not_found")

    status = "already_used" if row["is_redeemed"] else "valid"
    return ValidateTicketResponse(
        ticketId=row["place_id"],
        eventId=row["concert_id"],
        eventTitle=row["title"],
        venue=row["venue"],
        dateTime=row["date_time"],
        seat=row["seat_label"],
        sector=row["sector"],
        status=status,
    )


def offline_pack(conn: sqlite3.Connection) -> list[ValidateTicketResponse]:
    rows = conn.execute(
        """
        SELECT p.place_id, p.seat_label, p.sector, p.is_redeemed, c.id, c.title, c.venue, c.date_time
        FROM places p JOIN concerts c ON c.id = p.concert_id
        """
    ).fetchall()
    out: list[ValidateTicketResponse] = []
    for r in rows:
        out.append(
            ValidateTicketResponse(
                ticketId=r["place_id"],
                eventId=r["id"],
                eventTitle=r["title"],
                venue=r["venue"],
                dateTime=r["date_time"],
                seat=r["seat_label"],
                sector=r["sector"],
                status="used" if r["is_redeemed"] else "valid",
            )
        )
    return out


def validate_gosuslugi_stub(session_id: str) -> tuple[str, str]:
    if "gosuslugi.ru/m" in session_id.lower():
        return "ok", "Демо: сессия похожа на цифровой ID (заглушка)"
    return "unknown", "Некорректный session_id для демо"