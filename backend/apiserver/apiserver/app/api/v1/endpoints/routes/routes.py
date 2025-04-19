"""
Эндпоинты для построения маршрутов.
"""
import asyncio
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Dict, Any
import requests
import time
from loguru import logger
from datetime import datetime

from apiserver.app.services.public_transport import PublicTransportService
from apiserver.app.services.transport_workload import TransportWorkloadService
from apiserver.config.settings import settings
from apiserver.app.api.v1.schemas.routes import RouteByCoordinatesRequest, RouteResponse


router = APIRouter()


@router.post("/create_routes")
async def create_routes_by_coordinates(request: RouteByCoordinatesRequest):
    """
    Получить оптимальные маршруты для пользователя
    """
    
    transport_service = PublicTransportService()
    
    # Находим остановки в радиусе от начальной точки
    stops_start = await transport_service.find_stops_in_radius(
        lat=request.start.latitude, 
        lon=request.start.longitude
    )
    stops_end = await transport_service.find_stops_in_radius(
        lat=request.end.latitude, 
        lon=request.end.longitude
    )
    logger.info(f"Найдено {len(stops_start)} остановок общественного транспорта у начальной точки")
    logger.info(f"Найдено {len(stops_end)} остановок общественного транспорта у конечной точки")

    # Проверяем, что найдены остановки как у начальной, так и у конечной точки
    if not stops_start or not stops_end:
        logger.warning("Не найдены остановки у начальной или конечной точки")
        return []

    # Создаем список для хранения всех найденных маршрутов
    all_routes = []

    # Для каждой пары остановок строим маршрут
    for start_stop in stops_start:
        start_point = start_stop.get('point', {})
        start_lat = start_point.get('lat')
        start_lon = start_point.get('lon')
        
        if not start_lat or not start_lon:
            continue
            
        for end_stop in stops_end:
            end_point = end_stop.get('point', {})
            end_lat = end_point.get('lat')
            end_lon = end_point.get('lon')

            if not end_lat or not end_lon:
                continue

            # Строим маршрут между остановками
            routes = await transport_service.build_public_transport_route(
                start_lat=start_lat,
                start_lon=start_lon,
                end_lat=end_lat,
                end_lon=end_lon
            )
            if routes:
                all_routes.extend(routes)

    # Возвращаем найденные маршруты
    logger.info(f"Всего построено {len(all_routes)} маршрутов")
    if len(all_routes) == 0:
        return []

    transport_workload_service = TransportWorkloadService()
    all_routes = transport_workload_service.set_routes_workload(all_routes)

    res = [0] * 3
    all_routes.sort(key=lambda x: x.get('total_duration', float('inf')))
    res[0] = all_routes[0]
    all_routes.sort(key=lambda x: x.get('workload', float('inf')))
    res[2] = all_routes[0]

    res[1] = transport_workload_service.get_pareto_optimal(all_routes)

    return res
