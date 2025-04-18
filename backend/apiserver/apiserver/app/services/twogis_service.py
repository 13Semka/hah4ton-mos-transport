from typing import Dict, List, Optional

from app.core.external.2gis.client import TwoGISClient, TwoGISSearchParams


class TwoGISService:
    """Сервис для работы с 2GIS API."""

    def __init__(self):
        self.client = TwoGISClient()

    async def search_organizations(
        self, query: str, region_id: Optional[int] = None, page: int = 1, page_size: int = 20
    ) -> Dict:
        """
        Поиск организаций по запросу.

        Args:
            query: Поисковый запрос
            region_id: ID региона
            page: Номер страницы
            page_size: Размер страницы

        Returns:
            Dict: Результаты поиска
        """
        params = TwoGISSearchParams(
            query=query,
            region_id=region_id,
            page=page,
            page_size=page_size,
        )
        return await self.client.search_organizations(params)

    async def get_organization_details(self, organization_id: str) -> Dict:
        """
        Получение детальной информации об организации.

        Args:
            organization_id: ID организации

        Returns:
            Dict: Детальная информация об организации
        """
        return await self.client.get_organization_details(organization_id) 