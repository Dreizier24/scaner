import time

from fastapi import FastAPI, HTTPException, WebSocket
from fastapi.middleware.cors import CORSMiddleware

from app.database import get_connection, init_db
from app import store
from app.schemas import (
    ConcertManifestResponse,
    ConcertsResponse,
    ExternalBarcodeImportRequest,
    GosuslugiIdValidateRequest,
    GosuslugiIdValidateResponse,
    PushkinRedeemRequest,
    PushkinRedeemResponse,
    RedemptionSyncRequest,
    RedemptionSyncResponse,
    SyncStateResponse,
    SyncTicketsResponse,
    ValidateTicketRequest,
    ValidateTicketResponse,
)
from app.app_release import read_update_manifest
from app.seed import ensure_loadtest_concert, seed_if_empty
from app.ws_hub import ws_hub

app = FastAPI(
    title="КОРА Scanner API",
    version="0.1.0",
    description="Демо-сервер для мобильного сканера билетов",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

_conn = get_connection()
init_db(_conn)
seed_if_empty(_conn)
ensure_loadtest_concert(_conn)


def db():
    return _conn


API_PREFIX = "/api/v1"


@app.get(f"{API_PREFIX}/concerts", response_model=ConcertsResponse)
def list_concerts():
    return ConcertsResponse(concerts=store.list_concerts(db()))


@app.get(f"{API_PREFIX}/concerts/{{concert_id}}/manifest", response_model=ConcertManifestResponse)
def get_manifest(concert_id: str, since: int | None = None):
    not_mod, revision, places = store.build_manifest(db(), concert_id, since)
    if revision == 0 and not places and not not_mod:
        raise HTTPException(status_code=404, detail="Концерт не найден")
    return ConcertManifestResponse(
        concertId=concert_id,
        revision=revision,
        notModified=not_mod,
        places=places,
    )


@app.get(f"{API_PREFIX}/concerts/{{concert_id}}/sync-state", response_model=SyncStateResponse)
def sync_state(concert_id: str):
    rev = store.get_concert_revision(db(), concert_id)
    if rev is None:
        raise HTTPException(status_code=404, detail="Концерт не найден")
    return SyncStateResponse(concertId=concert_id, revision=rev)


@app.post(
    f"{API_PREFIX}/concerts/{{concert_id}}/redemptions/sync",
    response_model=RedemptionSyncResponse,
)
async def sync_redemptions(concert_id: str, body: RedemptionSyncRequest):
    if store.get_concert_revision(db(), concert_id) is None:
        raise HTTPException(status_code=404, detail="Концерт не найден")
    accepted, events = store.apply_redemptions(db(), concert_id, body.items)
    for payload in events:
        await ws_hub.broadcast_redemption(concert_id, payload)
    rev = store.get_concert_revision(db(), concert_id) or 1
    return RedemptionSyncResponse(accepted=accepted, serverRevision=rev)


@app.websocket(f"{API_PREFIX}/ws/concerts/{{concert_id}}")
async def concert_scan_feed(concert_id: str, websocket: WebSocket):
    if store.get_concert_revision(db(), concert_id) is None:
        await websocket.close(code=4404)
        return
    await ws_hub.connect(concert_id, websocket)
    await ws_hub.keepalive_loop(concert_id, websocket)


@app.post(
    f"{API_PREFIX}/concerts/{{concert_id}}/barcodes/import",
    response_model=ConcertManifestResponse,
)
def import_barcodes(concert_id: str, body: ExternalBarcodeImportRequest):
    if store.get_concert_revision(db(), concert_id) is None:
        raise HTTPException(status_code=404, detail="Концерт не найден")
    store.import_barcodes(db(), concert_id, body.barcodes)
    _, revision, places = store.build_manifest(db(), concert_id, None)
    return ConcertManifestResponse(
        concertId=concert_id,
        revision=revision,
        places=places,
    )


@app.post(f"{API_PREFIX}/pushkin/redeem", response_model=PushkinRedeemResponse)
def pushkin_redeem(body: PushkinRedeemRequest):
    status, message = store.redeem_pushkin(
        db(),
        body.concertId,
        body.placeId,
        body.barcode,
        body.scannedAtEpochMs,
    )
    return PushkinRedeemResponse(status=status, message=message)


@app.post(f"{API_PREFIX}/tickets/validate", response_model=ValidateTicketResponse)
def validate_ticket(body: ValidateTicketRequest):
    return store.validate_ticket(db(), body.ticketId, body.rawPayload)


@app.get(f"{API_PREFIX}/tickets/offline-pack", response_model=SyncTicketsResponse)
def offline_pack():
    return SyncTicketsResponse(
        tickets=store.offline_pack(db()),
        syncedAtEpochMs=int(time.time() * 1000),
    )


@app.post(
    f"{API_PREFIX}/gosuslugi/digital-id/validate",
    response_model=GosuslugiIdValidateResponse,
)
def validate_digital_id(body: GosuslugiIdValidateRequest):
    status, details = store.validate_gosuslugi_stub(body.sessionId)
    return GosuslugiIdValidateResponse(status=status, details=details)


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/app/update.json")
def app_update_manifest():
    """Манифест OTA-обновления Android (versionCode, apkUrl)."""
    return read_update_manifest()