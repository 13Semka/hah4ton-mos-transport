"""
Эндпоинты для построения маршрутов.
"""
from fastapi import APIRouter
from loguru import logger

from apiserver.app.api.v1.schemas.routes import RouteByCoordinatesRequest, RouteResponse
from apiserver.app.services.public_transport import PublicTransportService
from apiserver.app.services.transport_workload import TransportWorkloadService

router = APIRouter()


@router.post("/build_routes")
async def build_routes_by_coordinates(
    request: RouteByCoordinatesRequest,
) -> RouteResponse:
    """
    Получить оптимальные маршруты для пользователя
    """

    transport_service = PublicTransportService()

    # Находим остановки в радиусе от начальной точки
    initial_stops = await transport_service.find_nearest_stops(
        lat=request.start.latitude, lon=request.start.longitude
    )
    end_stops = await transport_service.find_nearest_stops(
        lat=request.end.latitude, lon=request.end.longitude
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
        return RouteResponse(
            fastest_route=None, balanced_route=None, least_crowded_route=None
        )

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
            end_stop_lat = end_stop["point"].get("lat")
            end_stop_lon = end_stop["point"].get("lon")

            if not end_stop_lat or not end_stop_lon:
                continue

            # Строим маршрут между остановками
            route = await transport_service.build_public_transport_route(
                start_lat=start_lat,
                start_lon=start_lon,
                end_lat=end_stop_lat,
                end_lon=end_stop_lon,
            )
            if route:
                all_routes.append(route)

    logger.info(f"Всего построено {len(all_routes)} маршрутов")
    if len(all_routes) == 0:
        logger.warning("Не найдены маршруты")
        return RouteResponse(
            fastest_route=None, balanced_route=None, least_crowded_route=None
        )

    transport_workload_service = TransportWorkloadService()
    all_routes_with_workload = transport_workload_service.set_routes_workload(
        all_routes
    )

    # Если маршрутов нет после расчета загруженности
    if not all_routes_with_workload:
        return RouteResponse(
            fastest_route=None, balanced_route=None, least_crowded_route=None
        )

    # Создаем список для результатов
    fastest_route = None
    least_crowded_route = None
    balanced_route = None

    # Сортируем по времени для получения самого быстрого маршрута
    all_routes_with_workload.sort(key=lambda x: x.get("total_duration", float("inf")))
    fastest_route = all_routes_with_workload[0]

    # Сортируем по загруженности для получения наименее загруженного маршрута
    all_routes_with_workload.sort(key=lambda x: x.get("workload", float("inf")))
    least_crowded_route = all_routes_with_workload[0]

    # Получаем Парето-оптимальные маршруты
    pareto_optimal_routes = transport_workload_service.get_pareto_optimal(
        all_routes_with_workload
    )
    if pareto_optimal_routes:
        # Выбираем первый из Парето-оптимальных маршрутов как сбалансированный
        balanced_route = pareto_optimal_routes[0]

    return RouteResponse(
        fastest_route=fastest_route,
        balanced_route=balanced_route,
        least_crowded_route=least_crowded_route,
    )
