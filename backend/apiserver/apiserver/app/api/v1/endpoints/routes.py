"""
Routes endpoints.
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from loguru import logger
from apiserver.app.services.geocoder import GeocoderService

router = APIRouter()
geocoder_service = GeocoderService()

class RouteRequest(BaseModel):
    start: str
    end: str

class RouteResponse(BaseModel):
    route: dict
    distance: float
    duration: float

@router.post("/route", response_model=RouteResponse)
async def get_route(request: RouteRequest):
    """
    Получает маршрут между двумя адресами используя 2GIS API
    """
    logger.info(f"Received route request: {request.start} -> {request.end}")
    
    route_data = await geocoder_service.get_route(request.start, request.end)
    
    if not route_data:
        logger.error("Failed to get route")
        raise HTTPException(status_code=400, detail="Could not build route between specified addresses")
    
    # Предполагаем, что 2GIS API возвращает данные в определенном формате
    # Возможно, потребуется адаптировать под реальный формат ответа API
    response = RouteResponse(
        route=route_data,
        distance=route_data.get("distance", 0),
        duration=route_data.get("duration", 0)
    )
    
    logger.info(f"Successfully built route: {request.start} -> {request.end}")
    return response 