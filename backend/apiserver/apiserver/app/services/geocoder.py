"""
Geocoder service.
"""
from typing import Dict, Optional, Tuple
from loguru import logger
from geopy.geocoders import Nominatim
from geopy.exc import GeocoderTimedOut
import httpx
from ...config.settings import settings

class GeocoderService:
    def __init__(self):
        self.geocoder = Nominatim(user_agent="hack_app")
        self.two_gis_api_key = settings.TWO_GIS_API_KEY
        self.two_gis_api_url = settings.TWO_GIS_API_URL

    async def geocode_address(self, address: str) -> Optional[Tuple[float, float]]:
        """
        Геокодирует адрес в координаты используя Nominatim
        """
        try:
            logger.info(f"Geocoding address: {address}")
            location = self.geocoder.geocode(address)
            if location:
                logger.info(f"Successfully geocoded address: {address} to {location.latitude}, {location.longitude}")
                return location.latitude, location.longitude
            logger.warning(f"Could not geocode address: {address}")
            return None
        except GeocoderTimedOut:
            logger.error(f"Timeout while geocoding address: {address}")
            return None
        except Exception as e:
            logger.error(f"Error while geocoding address: {address}. Error: {str(e)}")
            return None

    async def get_route(self, start: str, end: str) -> Optional[Dict]:
        """
        Получает маршрут между двумя точками используя 2GIS API
        """
        try:
            # Сначала геокодируем адреса
            start_coords = await self.geocode_address(start)
            end_coords = await self.geocode_address(end)

            if not start_coords or not end_coords:
                logger.error("Could not geocode one or both addresses")
                return None

            # Формируем URL для запроса к 2GIS API
            url = f"{self.two_gis_api_url}/route"
            params = {
                "key": self.two_gis_api_key,
                "start": f"{start_coords[0]},{start_coords[1]}",
                "end": f"{end_coords[0]},{end_coords[1]}",
                "mode": "car"  # Можно добавить другие режимы: walking, transit
            }

            logger.info(f"Requesting route from 2GIS API: {start} -> {end}")
            async with httpx.AsyncClient() as client:
                response = await client.get(url, params=params)
                response.raise_for_status()
                route_data = response.json()

            logger.info("Successfully received route from 2GIS API")
            return route_data

        except httpx.HTTPError as e:
            logger.error(f"HTTP error while getting route: {str(e)}")
            return None
        except Exception as e:
            logger.error(f"Error while getting route: {str(e)}")
            return None 