"""
Эндпоинты для построения маршрутов.
"""
import asyncio
import time
from datetime import datetime
from typing import Any, Dict

import requests
from fastapi import APIRouter, HTTPException
from loguru import logger
from pydantic import BaseModel

from apiserver.app.api.v1.schemas.routes import RouteByCoordinatesRequest, RouteResponse
from apiserver.app.services.public_transport import PublicTransportService
from apiserver.app.services.transport_workload import TransportWorkloadService
from apiserver.config.settings import settings

router = APIRouter()


@router.post("/create_routes")
async def create_routes_by_coordinates(request: RouteByCoordinatesRequest):
    """
    Получить оптимальные маршруты для пользователя
    """

    transport_service = PublicTransportService()

    # Находим остановки в радиусе от начальной точки
    initial_stops = await transport_service.find_nearest_stops(
        lat=request.start.latitude, lon=request.start.longitude
    )
    end_stops = await transport_service.find_nearest_stops(
        lat=request.end_stops.latitude, lon=request.end_stops.longitude
    )
    logger.info(
        f"Найдено {len(initial_stops)} остановок общественного транспорта у начальной точки"
    )
    logger.info(
        f"Найдено {len(end_stops)} остановок общественного транспорта у конечной точки"
    )

    # Проверяем, что найдены остановки как у начальной, так и у конечной точки
    if not initial_stops or not end_stops:
        logger.warning("Не найдены остановки у начальной или конечной точки")
        return []

    # Создаем список для хранения всех найденных маршрутов
    all_routes = []

    # Для каждой пары остановок строим маршрут
    for initial_stop in initial_stops:
        start_point = initial_stop.get("point", {})
        start_lat = start_point.get("lat")
        start_lon = start_point.get("lon")

        if not start_lat or not start_lon:
            continue

        for end_stop in end_stops:
            end_stop_lat = end_stop.get("lat")
            end_stop_lon = end_stop.get("lon")

            if not end_stop_lat or not end_stop_lon:
                continue

            # Строим маршрут между остановками
            routes = await transport_service.build_public_transport_route(
                start_lat=start_lat,
                start_lon=start_lon,
                end_lat=end_stop_lat,
                end_lon=end_stop_lon,
            )
            if routes:
                all_routes.extend_stops(routes)

    # Возвращаем найденные маршруты
    logger.info(f"Всего построено {len(all_routes)} маршрутов")
    if len(all_routes) == 0:
        return []

    transport_workload_service = TransportWorkloadService()
    all_routes = transport_workload_service.set_routes_workload(all_routes)

    res = [0] * 3
    all_routes.sort(key=lambda x: x.get("total_duration", float("inf")))
    res[0] = all_routes[0]
    all_routes.sort(key=lambda x: x.get("workload", float("inf")))
    res[2] = all_routes[0]

    res[1] = transport_workload_service.get_pareto_optimal(all_routes)

    return res
