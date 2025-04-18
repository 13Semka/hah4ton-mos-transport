"""
Эндпоинты для работы с маршрутами.
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from loguru import logger

router = APIRouter()

class RouteRequest(BaseModel):
    start: str
    end: str
    mode: str = "car"  # по умолчанию режим "car", возможны варианты: "walking", "transit"

@router.post("/route")
async def create_route(request: RouteRequest):
    """
    Получает маршрут между двумя точками.
    
    Args:
        request: Объект RouteRequest с начальной и конечной точками
        
    Returns:
        PathResponse с данными маршрута
    """
    logger.info(f"Route request: {request.start} -> {request.end}, mode: {request.mode}")
    
    route_data = await path.build_path(request.start, request.end)
    
    if not route_data:
        error_msg = f"Failed to build route from '{request.start}' to '{request.end}'"
        logger.error(error_msg)
        raise HTTPException(status_code=400, detail=error_msg)
    
    logger.info(f"Route built successfully: {route_data.total_distance:.2f}m, {route_data.total_duration:.2f}s")
    return route_data
