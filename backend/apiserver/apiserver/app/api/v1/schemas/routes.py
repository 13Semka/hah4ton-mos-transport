from typing import Dict, List

from pydantic import BaseModel


class Coordinates(BaseModel):
    latitude: float
    longitude: float


class RouteByCoordinatesRequest(BaseModel):
    start: Coordinates
    end: Coordinates
    transport_type: str = "public_transport"


class RouteResponse(BaseModel):
    """Ответ с маршрутами"""

    fastest_route: dict | None
    balanced_route: dict | None
    least_crowded_route: dict | None
