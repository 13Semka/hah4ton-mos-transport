from typing import Dict, Optional

import httpx
from pydantic import BaseModel

from config.settings import settings


class TwoGISSearchParams(BaseModel):
    """Параметры поиска для 2GIS API."""
    query: str
    region_id: Optional[int] = None
    page: Optional[int] = 1
    page_size: Optional[int] = 20


class TwoGISClient:
    """Клиент для работы с 2GIS API."""

    def __init__(self):
        self.api_key = settings.API_KEY_2GIS
        self.base_url = settings.API_URL_2GIS
        self.headers = {
            "Authorization": f"Key {self.api_key}",
            "Content-Type": "application/json",
        }

    async def search_organizations(self, params: TwoGISSearchParams) -> Dict:
        """
        Поиск организаций в 2GIS.

        Args:
            params: Параметры поиска

        Returns:
            Dict: Результаты поиска
        """
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.base_url}/items",
                headers=self.headers,
                params=params.dict(exclude_none=True),
            )
            response.raise_for_status()
            return response.json()

    async def get_organization_details(self, organization_id: str) -> Dict:
        """
        Получение детальной информации об организации.

        Args:
            organization_id: ID организации в 2GIS

        Returns:
            Dict: Детальная информация об организации
        """
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.base_url}/items/{organization_id}",
                headers=self.headers,
            )
            response.raise_for_status()
            return response.json() 