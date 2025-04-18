"""
Application settings module.
"""
from pydantic_settings import BaseSettings
from typing import Optional

class Settings(BaseSettings):
    # Основные настройки приложения
    ENVIRONMENT: str = "development"
    DEBUG: bool = True
    API_V1_STR: str = "/api/v1"
    PROJECT_NAME: str = "API Server"
    
    # Настройки 2GIS API
    TWO_GIS_API_KEY: str
    TWO_GIS_API_URL: str = "https://2gis.ru/api/v1"
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = True

settings = Settings() 