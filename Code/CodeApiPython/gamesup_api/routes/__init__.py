"""Routes HTTP du service."""

from gamesup_api.routes.health import router as health_router
from gamesup_api.routes.recommendations import router as recommendations_router

__all__ = ["health_router", "recommendations_router"]
