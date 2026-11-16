import json
from collections import defaultdict

from fastapi import WebSocket
from starlette.websockets import WebSocketDisconnect


class ConcertWsHub:
    """Рассылка погашений всем сканерам на одном мероприятии."""

    def __init__(self) -> None:
        self._rooms: dict[str, set[WebSocket]] = defaultdict(set)

    async def connect(self, concert_id: str, websocket: WebSocket) -> None:
        await websocket.accept()
        self._rooms[concert_id].add(websocket)

    def disconnect(self, concert_id: str, websocket: WebSocket) -> None:
        self._rooms[concert_id].discard(websocket)
        if not self._rooms[concert_id]:
            del self._rooms[concert_id]

    async def broadcast_redemption(self, concert_id: str, payload: dict) -> None:
        body = json.dumps(payload, ensure_ascii=False)
        dead: list[WebSocket] = []
        for ws in list(self._rooms.get(concert_id, ())):
            try:
                await ws.send_text(body)
            except Exception:
                dead.append(ws)
        for ws in dead:
            self.disconnect(concert_id, ws)

    async def keepalive_loop(self, concert_id: str, websocket: WebSocket) -> None:
        try:
            while True:
                await websocket.receive_text()
        except WebSocketDisconnect:
            self.disconnect(concert_id, websocket)


ws_hub = ConcertWsHub()