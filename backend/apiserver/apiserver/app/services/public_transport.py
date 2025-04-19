"""
Сервис для построения маршрутов в 2gis.
Предоставляет функциональность для поиска остановок общественного транспорта.
"""
import math
from typing import Any, Coroutine, Dict, List, Optional

import httpx
from fastapi import HTTPException
from loguru import logger

from apiserver.config.settings import settings


class PublicTransportService:
    """Сервис для построения маршрутов в 2gis."""

    def __init__(self):
        """Инициализация сервиса."""
        self.api_key = settings.get("GIS_API_KEY")
        self.base_url = "https://catalog.api.2gis.com/3.0/items"
        self.search_radius_const = settings.get("TRANSPORT_SEARCH_RADIUS", 500)  # метры

    async def find_nearest_stop(
        self, lat: float, lon: float
    ) -> Optional[Dict[str, Any]]:
        """
        Находит ближайшую остановку общественного транспорта.

        Args:
            lat: Широта точки
            lon: Долгота точки

        Returns:
            Информация о ближайшей остановке или None, если остановки не найдены
        """
        params = {
            "q": "остановка автобуса",
            "point": f"{lon},{lat}",
            "fields": "items.point",
            "key": self.api_key,
            "sort": "distance",
            "radius": 10000,
            "type": "station",
            "limit": 50,
        }

        async with httpx.AsyncClient() as client:
            response = await client.get(self.base_url, params=params)
            if response.status_code == 200:
                data = response.json()
                if data.get("result", {}).get("items", []):
                    return data["result"]["items"][0]
                return None
            else:
                logger.error(
                    f"Ошибка при поиске ближайшей остановки: {response.status_code}"
                )
                logger.debug(response.text)
                raise HTTPException(status_code=500, detail=response.text)

    @staticmethod
    def calculate_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
        """
        Вычисляет расстояние между двумя точками на земной поверхности.

        Args:
            lat1: Широта первой точки
            lon1: Долгота первой точки
            lat2: Широта второй точки
            lon2: Долгота второй точки

        Returns:
            Расстояние в метрах
        """
        # Радиус Земли в метрах
        earth_radius = 6371000

        # Перевод градусов в радианы
        lat1_rad = math.radians(lat1)
        lon1_rad = math.radians(lon1)
        lat2_rad = math.radians(lat2)
        lon2_rad = math.radians(lon2)

        # Разница координат
        dlat = lat2_rad - lat1_rad
        dlon = lon2_rad - lon1_rad

        # Формула гаверсинусов
        a = (
            math.sin(dlat / 2) ** 2
            + math.cos(lat1_rad) * math.cos(lat2_rad) * math.sin(dlon / 2) ** 2
        )
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))

        # Расстояние в метрах
        distance = earth_radius * c

        return distance

    async def find_nearest_stops(self, lat: float, lon: float) -> List[Dict[str, Any]]:
        """
        Находит ближайшую остановку, а затем все остановки в радиусе (расстояние до ближайшей + константа).

        Args:
            lat: Широта точки
            lon: Долгота точки

        Returns:
            Список остановок с их координатами
        """
        # Находим ближайшую остановку
        nearest_stop = await self.find_nearest_stop(lat, lon)

        if not nearest_stop:
            return []

        # Получаем координаты ближайшей остановки
        stop_point = nearest_stop.get("point", {})
        stop_lat = stop_point.get("lat")
        stop_lon = stop_point.get("lon")

        if not stop_lat or not stop_lon:
            return []

        # Вычисляем расстояние до ближайшей остановки
        distance_to_nearest = self.calculate_distance(lat, lon, stop_lat, stop_lon)

        # Определяем радиус поиска (расстояние до ближайшей + константа)
        search_radius = distance_to_nearest + self.search_radius_const

        params = {
            "q": "остановка автобуса",
            "point": f"{lon},{lat}",
            "fields": "items.point",
            "key": self.api_key,
            "sort": "distance",
            "type": "station",
            "radius": int(search_radius),
            "limit": settings.get("MAX_STOP_COUNT", 5),
        }

        async with httpx.AsyncClient() as client:
            response = await client.get(self.base_url, params=params)
            if response.status_code == 200:
                data = response.json()
                stops = data.get("result", {}).get("items", [])

                return stops

            return []

    async def build_public_transport_route(
        self, start_lat, start_lon, end_lat, end_lon
    ) -> Any | None:
        """
        Строит маршрут через общественный транспорт с использованием API 2GIS.

        Args:
            start_lat: Широта начальной точки
            start_lon: Долгота начальной точки
            end_lat: Широта конечной точки
            end_lon: Долгота конечной точки

        Returns:
            Информация о маршруте или None в случае ошибки
        """

        route_url = "https://routing.api.2gis.com/public_transport/2.0"

        # Формируем запрос к API 2GIS для построения маршрута общественного транспорта
        params = {"key": self.api_key}

        payload = {
            "max_result_count": 1,
            "source": {"point": {"lat": start_lat, "lon": start_lon, "type": "stop"}},
            "target": {"point": {"lat": end_lat, "lon": end_lon, "type": "stop"}},
            "enable_schedule": True,
            "transport": [
                "pedestrian",
                "metro",
                "light_metro",
                "suburban_train",
                "aeroexpress",
                "tram",
                "bus",
                "trolleybus",
                "shuttle_bus",
                "monorail",
                "funicular_railway",
                "river_transport",
                "cable_car",
                "light_rail",
                "premetro",
                "mcc",
                "mcd",
            ],
            "locale": "ru_RU",
        }

        try:
            async with httpx.AsyncClient() as client:
                response = await client.post(route_url, params=params, json=payload)
                if response.status_code == 200:
                    routes = response.json()
                    return routes[0]
                logger.error(f"Ошибка при построении маршрута: {response.text}")
                return None
        except Exception as e:
            logger.error(f"Ошибка при построении маршрута: {e}")
            return None
