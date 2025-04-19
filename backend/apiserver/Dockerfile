FROM python:3.13-slim
# Set environment variables
ENV PYTHONFAULTHANDLER=1 \
    PYTHONHASHSEED=random \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_DEFAULT_TIMEOUT=100 \
    POETRY_VERSION=1.7.1 \
    POETRY_NO_INTERACTION=1 \
    POETRY_VIRTUALENVS_CREATE=false \
    API_ENV=production

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Install Poetry
RUN pip install "poetry==$POETRY_VERSION"

# Copy only requirements to cache them in docker layer
COPY pyproject.toml poetry.lock* /app/

# Install dependencies
RUN poetry install --only main --no-root

# Copy config files
COPY apiserver/config/settings.toml /app/apiserver/config/
COPY apiserver/config/.secrets.toml.example /app/apiserver/config/.secrets.toml

# Copy application code
COPY . /app/

# Create logs directory
RUN mkdir -p /app/logs

# Create a non-root user and switch to it
RUN adduser --disabled-password --gecos "" appuser
RUN chown -R appuser:appuser /app
USER appuser

# Run application
CMD ["uvicorn", "apiserver.main:app", "--host", "0.0.0.0", "--port", "8000"]
