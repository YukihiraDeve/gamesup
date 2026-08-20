"""Point d'entrée ASGI du service de recommandation."""

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from gamesup_api import __version__
from gamesup_api.config import Settings, get_settings
from gamesup_api.routes.health import router as health_router


def create_app(settings: Settings | None = None) -> FastAPI:
    """Construit l'application FastAPI."""

    application_settings = settings or get_settings()
    application = FastAPI(
        title="GamesUP Recommendation API",
        version=__version__,
        docs_url=None,
        redoc_url=None,
        openapi_url=None,
    )
    application.state.settings = application_settings
    application.include_router(health_router)

    @application.exception_handler(Exception)
    async def handle_unexpected_error(
        _request: Request, _error: Exception
    ) -> JSONResponse:
        return JSONResponse(
            status_code=500,
            content={"detail": "Une erreur interne est survenue."},
        )

    return application


app = create_app()
