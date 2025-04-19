"""
Application settings module.
"""
from dynaconf import Dynaconf
from typing import Optional

# Инициализация настроек с помощью Dynaconf
settings = Dynaconf(
    envvar_prefix="API",
    settings_files=[
        "apiserver/config/settings.toml",  # Основные настройки
        "apiserver/config/.secrets.toml",  # Секретные настройки
    ],
    environments=True,  # Включает поддержку разных окружений (development, production)
    load_dotenv=False,  # Не загружать .env файл, используем .secrets.toml
    env_switcher="API_ENV",  # Переменная окружения для переключения окружений
    includes=[],  # Дополнительные файлы, если потребуются
)
