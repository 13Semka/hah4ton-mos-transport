"""
Основной роутер для API v1.
"""
from fastapi import APIRouter

from .routes import routes
from .status import status

# Создаем основной роутер
endpoints_router = APIRouter()

# Подключаем роутер для работы со статусом
endpoints_router.include_router(status.router, prefix="", tags=["status"])

# Подключаем роутер для работы с маршрутами
endpoints_router.include_router(routes.router, prefix="/routes", tags=["routes"])
