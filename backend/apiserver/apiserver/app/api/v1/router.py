"""
Основной роутер для API v1.
"""
from fastapi import APIRouter

# Импортируем роутеры из endpoints
from .endpoints.routes.routes import router as routes_router

# Создаем основной роутер
api_router = APIRouter()

# Включаем все подроутеры
api_router.include_router(routes_router, prefix="/routes", tags=["routes"]) 