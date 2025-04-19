"""
Сервис для расчета загруженности транспорта.
"""
import random
from typing import Any, Dict, List

import httpx

from apiserver.config.settings import settings


class TransportWorkloadService:
    """
    Сервис для расчета и управления загруженностью транспорта.

    Предоставляет методы для получения информации о загруженности станций
    и маршрутов, а также для выбора оптимальных маршрутов по критериям
    загруженности и длительности.
    """

    def __init__(self):
        """Инициализация сервиса загруженности транспорта."""
        pass

    def get_station_workload(self) -> int:
        """
        Получает информацию о загруженности станции.

        Выполняет запрос к внешнему API для получения текущей
        загруженности станции.

        Returns:
            int: Числовое значение загруженности станции.
        """
        url = f"{settings.station_workload_url}/api/v1/workload"
        response = httpx.get(url)
        return response.json()["workload"]

    def set_routes_workload(self, routes: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Рассчитывает загруженность для списка маршрутов.

        Для каждого маршрута, который не является пешеходным,
        вычисляется загруженность на основе случайных значений и
        данных о загруженности станций, если доступен URL станции.

        Args:
            routes (List[Dict[str, Any]]): Список маршрутов, для которых нужно
                рассчитать загруженность.

        Returns:
            List[Dict[str, Any]]: Список маршрутов с добавленной информацией
                о загруженности.
        """
        routes_with_transport = []

        for route in routes:
            if route.get("pedestrian", False):
                continue
            routes_with_transport.append(route)
        for i in range(len(routes_with_transport)):
            workload_values = []
            for _ in range(
                len(routes_with_transport[i]["waypoints"][0]["routes_names"])
            ):
                transport_workload = random.randint(1, 60)
                combined_workload = transport_workload
                if settings.station_workload_url:
                    combined_workload = transport_workload + min(
                        self.get_station_workload(), 60 - transport_workload
                    )
                workload_values.append(combined_workload / 60)
            routes_with_transport[i]["workload"] = sum(workload_values) / len(
                workload_values
            )

        return routes_with_transport

    def get_pareto_optimal(self, routes: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Определяет Парето-оптимальные маршруты из списка маршрутов.

        Маршрут считается Парето-оптимальным, если не существует другого маршрута,
        который был бы лучше по обоим критериям (загруженность и время)
        или лучше по одному критерию и не хуже по другому.

        Args:
            routes (List[Dict[str, Any]]): Список маршрутов для анализа.

        Returns:
            List[Dict[str, Any]]: Список Парето-оптимальных маршрутов.
        """
        pareto = []
        for candidate in routes:
            dominated = False
            for other in routes:
                if (
                    other["workload"] < candidate["workload"]
                    and other["total_duration"] <= candidate["total_duration"]
                ) or (
                    other["workload"] <= candidate["workload"]
                    and other["total_duration"] < candidate["total_duration"]
                ):
                    dominated = True
                    break
            if not dominated:
                pareto.append(candidate)
        return pareto
