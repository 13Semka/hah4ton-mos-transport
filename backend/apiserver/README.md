# API Server

API сервер на FastAPI с интеграцией 2GIS для построения маршрутов.

## Настройка окружения

### Установка зависимостей

```bash
# Установка Poetry (если не установлен)
make install-poetry

# Установка зависимостей проекта
make install
```

### Настройка конфигурации

Приложение использует [Dynaconf](https://www.dynaconf.com/) для конфигурации. 

1. Скопируйте файл с шаблоном секретных настроек:

```bash
cp apiserver/config/.secrets.toml.example apiserver/config/.secrets.toml
```

2. Отредактируйте файл `.secrets.toml` и укажите в нем свои секретные данные:
   - API ключ для 2GIS
   - Данные для подключения к базе данных (при необходимости)
   - Секретный ключ для JWT токенов (при необходимости)

#### Пример конфигурации

**settings.toml** (общие настройки):
```toml
[default]
environment = "development"
debug = true
api_v1_str = "/api/v1"
project_name = "API Server"
two_gis_api_url = "https://2gis.ru/api/v1"

[development]
debug = true

[production]
debug = false
```

**.secrets.toml** (секретные настройки):
```toml
[default]
two_gis_api_key = "your_api_key_here"
secret_key = "your_super_secret_key_here"

[development]
two_gis_api_key = "development_key"

[production]
# В продакшене можно использовать переменные окружения
two_gis_api_key = "@format {env[TWO_GIS_API_KEY]}"
```

#### Переключение окружений

Вы можете переключаться между окружениями (development, testing, production) с помощью переменной окружения:

```bash
# Для разработки
export API_ENV=development

# Для продакшена
export API_ENV=production
```

## Запуск приложения

### Режим разработки

```bash
make run
```

### Режим продакшена

```bash
make run-prod
```

## API Эндпоинты

### Статус сервера

- `GET /api/v1/` - Корневой эндпоинт с информацией о сервисе
- `GET /api/v1/health` - Проверка работоспособности сервера
- `GET /api/v1/status` - Подробная информация о состоянии сервера

### Маршруты

- `POST /api/v1/routes/route` - Получение маршрута между двумя точками
- `GET /api/v1/routes/route/estimate` - Быстрая оценка маршрута (расстояние и время)

## Документация API

После запуска приложения документация доступна по адресу:
- Swagger UI: `http://localhost:8000/api/v1/docs`
- ReDoc: `http://localhost:8000/api/v1/redoc`

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