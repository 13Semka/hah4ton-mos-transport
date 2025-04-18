"""
Основной роутер для API v1.
"""
from fastapi import APIRouter
from .status import status
from .routes import routes

# Создаем основной роутер
api_router = APIRouter()

# Подключаем роутер для работы со статусом
api_router.include_router(
    status.router,
    prefix="",
    tags=["status"]
)

# Подключаем роутер для работы с маршрутами
api_router.include_router(
    routes.router,
    prefix="/routes",
    tags=["routes"]
)