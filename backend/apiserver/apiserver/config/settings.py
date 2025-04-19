"""
Application settings module.
"""
from typing import Optional

from dynaconf import Dynaconf

settings = Dynaconf(
    envvar_prefix="API",
    settings_files=[
        "apiserver/config/settings.toml",
        "apiserver/config/.secrets.toml",
    ],
    environments=True,  # Включает поддержку разных окружений (development, production)
    load_dotenv=True,
    env_switcher="API_ENV",  # Переменная окружения для переключения окружений
    includes=[],  # Дополнительные файлы, если потребуются
)
