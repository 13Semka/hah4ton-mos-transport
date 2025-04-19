"""
Сервис для расчета загруженности транспорта.
"""
import httpx
from fastapi import HTTPException
from typing import List, Dict, Any, Optional
import math
import random
from loguru import logger

from apiserver.config.settings import settings


class TransportWorkloadService:
    def __init__(self):
        return super().__init__()

    def get_station_workload(self) -> int:
        url = f"{settings.station_workload_url}/api/v1/workload"
        response = httpx.get(url)
        return response.json()['workload']
    
    def set_routes_workload(self, routes: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        routes_with_transport = []
        transport_numbers = []

        for route in routes:
            if route.get('pedestrian', False):
                continue
            routes_with_transport.append(route)
        for i in range(len(routes_with_transport)):
            workload_values = []
            for _ in range(len(routes_with_transport[i]['waypoints'][0]['routes_names'])):
                transport_workload = random.randint(1, 60)
                combined_workload = transport_workload
                if settings.station_workload_url:
                    combined_workload = transport_workload + min(self.get_station_workload(), 60 - transport_workload)
                workload_values.append(combined_workload/60)
            routes_with_transport[i]['workload'] = sum(workload_values) / len(workload_values)
        
        return routes_with_transport
    
    def get_pareto_optimal(self, routes: List[Dict[str, Any]]) ->  Dict[str, Any]:
        pareto = []
        for candidate in routes:
            dominated = False
            for other in routes:
                if (other['workload'] < candidate['workload'] and other['total_duration'] <= candidate['total_duration']) or \
                (other['workload'] <= candidate['workload'] and other['total_duration'] < candidate['total_duration']):
                    dominated = True
                    break
            if not dominated:
                pareto.append(candidate)
        return pareto
