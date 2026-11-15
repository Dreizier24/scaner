from typing import Optional

from pydantic import BaseModel


class ConcertDto(BaseModel):
    id: str
    title: str
    venue: Optional[str] = None
    dateTime: Optional[str] = None


class ConcertsResponse(BaseModel):
    concerts: list[ConcertDto] = []


class PlaceManifestDto(BaseModel):
    placeId: str
    row: Optional[str] = None
    seat: Optional[str] = None
    sector: Optional[str] = None
    barcodes: list[str] = []
    isPushkin: bool = False
    isRedeemed: bool = False


class ConcertManifestResponse(BaseModel):
    concertId: str
    revision: int
    notModified: bool = False
    places: list[PlaceManifestDto] = []


class SyncStateResponse(BaseModel):
    concertId: str
    revision: int


class RedemptionItemDto(BaseModel):
    placeId: str
    barcode: str
    isPushkin: bool
    scannedAtEpochMs: int


class RedemptionSyncRequest(BaseModel):
    concertId: str
    clientRevision: Optional[int] = None
    items: list[RedemptionItemDto] = []


class RedemptionSyncResponse(BaseModel):
    accepted: int
    serverRevision: int


class ExternalBarcodeImportDto(BaseModel):
    barcode: str
    placeId: Optional[str] = None
    row: Optional[str] = None
    seat: Optional[str] = None
    sector: Optional[str] = None
    isPushkin: bool = False


class ExternalBarcodeImportRequest(BaseModel):
    barcodes: list[ExternalBarcodeImportDto] = []


class PushkinRedeemRequest(BaseModel):
    concertId: str
    placeId: str
    barcode: str
    scannedAtEpochMs: int


class PushkinRedeemResponse(BaseModel):
    status: str
    message: Optional[str] = None


class ValidateTicketRequest(BaseModel):
    ticketId: Optional[str] = None
    rawPayload: str
    scannedAtEpochMs: int


class ValidateTicketResponse(BaseModel):
    ticketId: Optional[str] = None
    eventId: Optional[str] = None
    eventTitle: Optional[str] = None
    venue: Optional[str] = None
    dateTime: Optional[str] = None
    seat: Optional[str] = None
    sector: Optional[str] = None
    holderName: Optional[str] = None
    orderId: Optional[str] = None
    status: str = "unknown"


class SyncTicketsResponse(BaseModel):
    tickets: list[ValidateTicketResponse] = []
    syncedAtEpochMs: int


class GosuslugiIdValidateRequest(BaseModel):
    sessionId: str
    scannedAtEpochMs: int


class GosuslugiIdValidateResponse(BaseModel):
    status: str
    details: Optional[str] = None