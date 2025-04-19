"""
Эндпоинты для проверки состояния API.
"""
from fastapi import APIRouter
from pydantic import BaseModel
from typing import Dict, Any
import platform
import psutil
import time
from loguru import logger
from datetime import datetime

from apiserver.config.settings import settings
from apiserver.app.api.v1.schemas.status import RootResponse, HealthResponse, StatusResponse


router = APIRouter()

# Время запуска приложения
START_TIME = time.time()


@router.get("/", response_model=RootResponse)
async def root():
    """
    Корневой эндпоинт API.
    Возвращает приветственное сообщение и ссылки на документацию.
    """
    logger.debug("Root endpoint request received")
    
    return RootResponse(
        message="Welcome to API Server",
        docs_url=f"{settings.API_V1_STR}/docs",
        redoc_url=f"{settings.API_V1_STR}/redoc"
    )

@router.get("/health", response_model=HealthResponse)
async def health_check():
    """
    Простая проверка работоспособности приложения.
    Возвращает 200 OK, если приложение работает.
    """
    logger.debug("Health check request received")
    
    return HealthResponse(
        status="healthy",
        timestamp=datetime.now().isoformat()
    )

@router.get("/status", response_model=StatusResponse)
async def status():
    """
    Подробная информация о состоянии приложения.
    Возвращает информацию о системе, использовании ресурсов и времени работы.
    """
    logger.info("Status request received")
    
    # Получение информации о системе
    memory = psutil.virtual_memory()
    
    # Подготовка ответа
    response = StatusResponse(
        status="running",
        version="0.1.0",  # Версия из pyproject.toml
        environment=settings.ENVIRONMENT,
        uptime=time.time() - START_TIME,
        system_info={
            "platform": platform.platform(),
            "python_version": platform.python_version(),
            "cpu_count": psutil.cpu_count(),
            "hostname": platform.node()
        },
        memory_usage={
            "total": memory.total,
            "available": memory.available,
            "percent": memory.percent
        },
        cpu_usage=psutil.cpu_percent(interval=0.1)
    )
    
    logger.info(f"Status response: running in {settings.ENVIRONMENT} environment")
    return response 