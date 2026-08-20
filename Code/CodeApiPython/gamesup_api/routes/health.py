"""Route de supervision du service."""

from fastapi import APIRouter, Request

from gamesup_api import __version__
from gamesup_api.schemas.health import HealthResponse

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthResponse)
async def health(request: Request) -> HealthResponse:
    """Indique que le processus HTTP est disponible."""

    return HealthResponse(
        status="UP",
        version=__version__,
        model_status=request.app.state.model_status.value,
    )
