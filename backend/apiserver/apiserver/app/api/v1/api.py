"""
API router configuration.
"""
from fastapi import APIRouter
from .endpoints import routes

api_router = APIRouter()

# Подключаем роутеры из endpoints
api_router.include_router(routes.router, prefix="/routes", tags=["routes"]) 