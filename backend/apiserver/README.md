# API Server

Современный API сервер на FastAPI с интеграцией внешних сервисов (2GIS) и использованием Poetry для управления зависимостями.

## Структура проекта

```
backend/
├── app/
│   ├── api/
│   │   └── v1/
│   │       └── endpoints/
│   │           └── twogis.py      # API эндпоинты для 2GIS
│   │
│   ├── core/
│   │   └── external/
│   │       └── 2gis/
│   │           └── client.py      # Клиент для работы с 2GIS API
│   │
│   ├── services/
│   │   └── twogis_service.py      # Сервисный слой для 2GIS
│   │
│   └── main.py                    # Основной файл приложения
├── config/
│   ├── settings.py                # Конфигурация приложения
│   ├── settings.toml              # Основные настройки
│   └── .secrets.toml             # Секретные данные (не в git)
├── tests/                         # Тесты
├── .env                          # Переменные окружения
├── pyproject.toml                # Конфигурация Poetry
└── README.md                     # Документация
```

## Установка

1. Установите Poetry:
```bash
curl -sSL https://install.python-poetry.org | python3 -
```

2. Клонируйте репозиторий и перейдите в директорию проекта:
```bash
cd backend
```

3. Установите зависимости:
```bash
poetry install
```

4. Создайте файл `.env` и добавьте необходимые переменные окружения:
```env
APP_ENV=development
APP_DEBUG=true
APP_API_KEY_2GIS=your_2gis_api_key
```

## Запуск

Для запуска в режиме разработки:
```bash
poetry run uvicorn app.main:app --reload
```

Приложение будет доступно по адресу: http://localhost:8000

## API Documentation

После запуска приложения, документация API доступна по адресам:
- Swagger UI: http://localhost:8000/api/v1/docs
- ReDoc: http://localhost:8000/api/v1/redoc

## Доступные эндпоинты

### 2GIS API

- `GET /api/v1/2gis/search` - Поиск организаций
  - Параметры:
    - `query` (string) - Поисковый запрос
    - `region_id` (int, optional) - ID региона
    - `page` (int, default=1) - Номер страницы
    - `page_size` (int, default=20) - Размер страницы

- `GET /api/v1/2gis/organizations/{organization_id}` - Получение информации об организации
  - Параметры:
    - `organization_id` (string) - ID организации в 2GIS

## Разработка

### Линтинг и форматирование

```bash
# Форматирование кода
poetry run black .
poetry run isort .

# Проверка типов
poetry run mypy .

# Линтинг
poetry run flake8
```

### Тестирование

```bash
poetry run pytest
```

## Принципы проектирования

- SOLID принципы
- Чистая архитектура
- Dependency Injection
- Асинхронное программирование
- Типизация
- Документированный код 