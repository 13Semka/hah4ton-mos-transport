from typing import Optional

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel

from app.services.twogis_service import TwoGISService

router = APIRouter()


class SearchResponse(BaseModel):
    """Модель ответа для поиска организаций."""
    total: int
    items: list


@router.get("/search", response_model=SearchResponse)
async def search_organizations(
    query: str,
    region_id: Optional[int] = None,
    page: int = 1,
    page_size: int = 20,
    twogis_service: TwoGISService = Depends(lambda: TwoGISService()),
):
    """
    Поиск организаций в 2GIS.

    Args:
        query: Поисковый запрос
        region_id: ID региона
        page: Номер страницы
        page_size: Размер страницы
        twogis_service: Сервис для работы с 2GIS

    Returns:
        SearchResponse: Результаты поиска
    """
    try:
        result = await twogis_service.search_organizations(
            query=query,
            region_id=region_id,
            page=page,
            page_size=page_size,
        )
        return SearchResponse(**result)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/organizations/{organization_id}")
async def get_organization_details(
    organization_id: str,
    twogis_service: TwoGISService = Depends(lambda: TwoGISService()),
):
    """
    Получение детальной информации об организации.

    Args:
        organization_id: ID организации
        twogis_service: Сервис для работы с 2GIS

    Returns:
        Dict: Детальная информация об организации
    """
    try:
        return await twogis_service.get_organization_details(organization_id)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e)) 