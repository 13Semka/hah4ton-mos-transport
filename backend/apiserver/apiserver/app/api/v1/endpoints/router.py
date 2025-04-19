"""
Основной роутер для API v1.
"""
from fastapi import APIRouter

from apiserver.config.settings import settings

from .routes import routes
from .status import status

# Создаем основной роутер
api_router = APIRouter(prefix=settings.get("api_v1_str", "/api/v1"))

# Подключаем роутер для работы со статусом
api_router.include_router(status.router, prefix="", tags=["status"])

# Подключаем роутер для работы с маршрутами
api_router.include_router(routes.router, prefix="/routes", tags=["routes"])
