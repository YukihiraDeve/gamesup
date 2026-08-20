"""Point d'entrée ASGI du service de recommandation."""

from asyncio import Lock
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from gamesup_api import __version__
from gamesup_api.config import Settings, get_settings
from gamesup_api.model import (
    IncompatibleModelArtifactError,
    InvalidModelArtifactError,
    ModelArtifactNotFoundError,
    ModelStatus,
    load_recommender,
)
from gamesup_api.routes import health_router, recommendations_router


@asynccontextmanager
async def model_lifespan(application: FastAPI) -> AsyncIterator[None]:
    """Charge sans exposer l'éventuelle erreur de l'artefact local."""

    artifact_path = application.state.settings.model_artifact_path
    try:
        application.state.recommender = load_recommender(artifact_path)
        application.state.model_status = ModelStatus.READY
    except ModelArtifactNotFoundError:
        application.state.model_status = ModelStatus.NOT_TRAINED
    except InvalidModelArtifactError:
        application.state.model_status = ModelStatus.INVALID
    except IncompatibleModelArtifactError:
        application.state.model_status = ModelStatus.INCOMPATIBLE
    yield


def create_app(settings: Settings | None = None) -> FastAPI:
    """Construit l'application FastAPI."""

    application_settings = settings or get_settings()
    application = FastAPI(
        title="GamesUP Recommendation API",
        version=__version__,
        docs_url=None,
        redoc_url=None,
        openapi_url=None,
        lifespan=model_lifespan,
    )
    application.state.settings = application_settings
    application.state.recommender = None
    application.state.model_status = ModelStatus.UNINITIALIZED
    application.state.training_lock = Lock()
    application.include_router(health_router)
    application.include_router(recommendations_router)

    @application.exception_handler(RequestValidationError)
    async def handle_request_validation_error(
        _request: Request, error: RequestValidationError
    ) -> JSONResponse:
        safe_errors = [
            {
                key: value
                for key, value in validation_error.items()
                if key in {"type", "loc", "msg"}
            }
            for validation_error in error.errors()
        ]
        return JSONResponse(
            status_code=422,
            content={"detail": safe_errors},
        )

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
