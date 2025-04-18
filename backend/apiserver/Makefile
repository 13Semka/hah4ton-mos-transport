.PHONY: install run clean test lint format install-poetry

# Variables
POETRY := poetry
PYTHON := $(POETRY) run python
UVICORN := $(POETRY) run uvicorn
APP_NAME := apiserver
APP_PATH := apiserver.main:app

# Install Poetry if not installed
install-poetry:
	curl -sSL https://install.python-poetry.org | python3 -

# Install dependencies
install:
	$(POETRY) install

# Run application in development mode
run:
	$(UVICORN) $(APP_PATH) --reload --host 0.0.0.0 --port 8000

# Run application in production mode
run-prod:
	$(UVICORN) $(APP_PATH) --host 0.0.0.0 --port 8000

# Clean cache and temporary files
clean:
	find . -type d -name "__pycache__" -exec rm -rf {} +
	find . -type d -name ".pytest_cache" -exec rm -rf {} +
	find . -type d -name ".mypy_cache" -exec rm -rf {} +
	find . -type d -name ".coverage" -exec rm -rf {} +
	find . -type d -name "htmlcov" -exec rm -rf {} +
	find . -type f -name "*.pyc" -delete
	find . -type f -name "*.pyo" -delete
	find . -type f -name "*.pyd" -delete
	find . -type f -name ".coverage" -delete
	find . -type f -name "coverage.xml" -delete

# Run tests
test:
	$(POETRY) run pytest

# Run linters
lint:
	$(POETRY) run flake8 .
	$(POETRY) run mypy .
	$(POETRY) run black . --check
	$(POETRY) run isort . --check-only

# Format code
format:
	$(POETRY) run black .
	$(POETRY) run isort .

# Update dependencies
update:
	$(POETRY) update

# Create new virtual environment
venv:
	$(POETRY) env remove --all
	$(POETRY) install

# Help
help:
	@echo "Available commands:"
	@echo "  make install-poetry - Install Poetry package manager"
	@echo "  make install     - Install dependencies"
	@echo "  make run        - Run application in development mode"
	@echo "  make run-prod   - Run application in production mode"
	@echo "  make clean      - Clean cache and temporary files"
	@echo "  make test       - Run tests"
	@echo "  make lint       - Run linters"
	@echo "  make format     - Format code"
	@echo "  make update     - Update dependencies"
	@echo "  make venv       - Create new virtual environment"
	@echo "  make help       - Show this message" 