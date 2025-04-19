# Деплой API-сервера

В этой директории содержатся файлы для развертывания API-сервера с использованием Docker и docker-compose.

## Требования

- Docker
- Docker Compose
- Ключ API для 2GIS

## Подготовка к запуску

1. Скопируйте файл `.env.example` в `.env`:

```bash
cp .env.example .env
```

2. Отредактируйте файл `.env` и укажите ваш API-ключ для 2GIS:

```
GIS_API_KEY=ваш_ключ_api
```

## Запуск контейнеров

Для запуска API-сервера выполните:

```bash
docker-compose up -d
```

Для остановки:

```bash
docker-compose down
```

## Проверка работоспособности

После запуска API-сервер будет доступен по адресу http://localhost:8000.

Документация API доступна по адресу:
- Swagger UI: http://localhost:8000/api/v1/docs
- ReDoc: http://localhost:8000/api/v1/redoc

Вы можете проверить статус сервера, выполнив запрос:

```bash
curl http://localhost:8000/api/v1/status/health
```

## Логи

Логи API-сервера сохраняются в томе Docker. Для просмотра логов выполните:

```bash
docker-compose logs -f apiserver
```

## Обновление

Для обновления API-сервера:

1. Остановите контейнеры:
```bash
docker-compose down
```

2. Пересоберите образы:
```bash
docker-compose build
```

3. Запустите контейнеры снова:
```bash
docker-compose up -d
``` 