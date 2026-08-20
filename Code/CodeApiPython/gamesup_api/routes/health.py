"""Route de supervision du service."""

from fastapi import APIRouter

from gamesup_api import __version__
from gamesup_api.schemas.health import HealthResponse

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    """Indique que le processus HTTP est disponible."""

    return HealthResponse(status="UP", version=__version__)
