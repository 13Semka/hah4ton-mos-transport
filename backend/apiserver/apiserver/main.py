"""
Main application module.
"""
import os
import sys
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from loguru import logger

from apiserver.app.api.v1.endpoints.router import api_router
from apiserver.config.settings import settings

# Настройка логирования
logger.remove()  # Удаляем стандартный обработчик
logger.add(
    sys.stdout,
    format="<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | <cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - <level>{message}</level>",
    level=settings.log_level,
)

# Создаем директорию для логов, если её нет
os.makedirs("logs", exist_ok=True)

logger.add(
    "logs/app.log",
    rotation=settings.log_rotation_period,
    retention=settings.log_retention_period,
    format="{time:YYYY-MM-DD HH:mm:ss} | {level: <8} | {name}:{function}:{line} - {message}",
    level=settings.log_level,
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Код выполняется при запуске приложения
    logger.info("Application startup")
    logger.info(f"API Documentation: http://localhost:8000{settings.API_V1_STR}/docs")

    yield  # Здесь приложение работает и обрабатывает запросы

    # Код выполняется при завершении работы приложения
    logger.info("Application shutdown")


app = FastAPI(
    title=settings.PROJECT_NAME,
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    docs_url=f"{settings.API_V1_STR}/docs",
    redoc_url=f"{settings.API_V1_STR}/redoc",
    lifespan=lifespan,
)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Подключаем API роутер
app.include_router(api_router)
