from typing import Any, Dict

from pydantic import BaseModel


class HealthResponse(BaseModel):
    status: str
    timestamp: str


class StatusResponse(BaseModel):
    status: str
    version: str
    environment: str
    uptime: float
    system_info: Dict[str, Any]
    memory_usage: Dict[str, Any]
    cpu_usage: float


class RootResponse(BaseModel):
    message: str
    docs_url: str
    redoc_url: str
