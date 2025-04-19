"""
Основной роутер для API v1.
"""
from fastapi import APIRouter

from apiserver.config.settings import settings

from .endpoints.router import endpoints_router

api_router = APIRouter(prefix=settings.API_V1_STR)

api_router.include_router(endpoints_router)
