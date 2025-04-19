from pydantic import BaseModel
from typing import Dict, List


class Coordinates(BaseModel):
    latitude: float
    longitude: float

class RouteByCoordinatesRequest(BaseModel):
    start: Coordinates
    end: Coordinates
    transport_type: str = "public_transport"

class RouteResponse(BaseModel):
    distance: float
    duration: int
    ui_distance: str
    ui_duration: str
    points: List[Dict]
