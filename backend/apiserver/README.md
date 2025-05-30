# API Сервер

## Описание

API Сервер - это бэкенд-сервис для построения оптимальных маршрутов общественного транспорта с учетом загруженности. Сервис предоставляет API для:
- Построения маршрутов между двумя точками
- Получения информации о загруженности транспорта
- Выбора оптимальных маршрутов по критериям времени и загруженности

## Технологии

- Python 3.10+
- FastAPI
- Poetry для управления зависимостями
- 2GIS API для построения маршрутов

## Установка и запуск

### Предварительные требования

- Python 3.10 или выше
- Poetry (менеджер пакетов)

### Шаги по установке

1. Клонировать репозиторий:
   ```bash
   git clone <url-репозитория>
   cd backend/apiserver
   ```

2. Установить Poetry (если не установлен):
   ```bash
   make install-poetry
   ```

3. Создать виртуальное окружение и установить зависимости:
   ```bash
   poetry install
   ```
4. Создать файл конфигурации `.secrets.toml` в директории config (можно просто переименовать .example файл)

5. Получить API ключ 2GIS:
   - Зарегистрируйтесь на [dev.2gis.ru](https://dev.2gis.ru/)
   - Создайте новое приложение в личном кабинете
   - Получите API ключ и добавьте его в файл `.secrets.toml`

6. Запуск сервера:
   ```bash
   # Запуск в режиме разработки
   make run
   
   # Запуск в производственном режиме
   make run-prod
   ```

  Сервер будет доступен по адресу http://localhost:8000 (см. [Makefile](./Makefile))
   
  Документация API доступна по адресу:
   - Swagger UI: http://localhost:8000/api/v1/docs
   - ReDoc: http://localhost:8000/api/v1/redoc


Example of request body coordinates:
```json
{
  "start": {
    "latitude": 43.401056,
    "longitude": 39.956288
  },
  "end": {
    "latitude": 43.401544,
    "longitude": 39.978169
  },
  "transport_type": "public_transport"
}
```