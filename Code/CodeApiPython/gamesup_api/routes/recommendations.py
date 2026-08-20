"""Routes internes d'entraînement et de recommandation."""

from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Request, status
from starlette.concurrency import run_in_threadpool

from gamesup_api.data import InsufficientTrainingDataError
from gamesup_api.model import KnnRecommender, ModelStatus
from gamesup_api.schemas import (
    RecommendationItem,
    RecommendationRequest,
    RecommendationResponse,
    TrainModelResponse,
    TrainingBatch,
)
from gamesup_api.security import require_internal_service
from gamesup_api.services import recommend, train_and_persist

InternalService = Annotated[None, Depends(require_internal_service)]

router = APIRouter(tags=["recommendations"])


@router.post(
    "/model/train",
    response_model=TrainModelResponse,
    responses={422: {"description": "Données insuffisantes ou invalides"}},
)
async def train_model(
    batch: TrainingBatch,
    request: Request,
    _internal_service: InternalService,
) -> TrainModelResponse:
    """Entraîne hors du thread événementiel puis active le nouveau modèle."""

    settings = request.app.state.settings
    async with request.app.state.training_lock:
        try:
            recommender = await run_in_threadpool(
                train_and_persist,
                batch,
                settings.knn_neighbors,
                settings.model_artifact_path,
            )
        except InsufficientTrainingDataError as error:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="training dataset is insufficient",
            ) from error

        request.app.state.recommender = recommender
        request.app.state.model_status = ModelStatus.READY

    statistics = recommender.artifact.statistics
    return TrainModelResponse(
        version=recommender.model_version,
        users=statistics.users,
        games=statistics.games,
        retained_interactions=statistics.retained_interactions,
    )


@router.post(
    "/recommendations",
    response_model=RecommendationResponse,
    responses={503: {"description": "Modèle indisponible"}},
)
async def get_recommendations(
    payload: RecommendationRequest,
    request: Request,
    _internal_service: InternalService,
) -> RecommendationResponse:
    """Retourne des recommandations ordonnées pour un utilisateur technique."""

    recommender: KnnRecommender | None = request.app.state.recommender
    if recommender is None:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="recommendation model is not available",
        )

    recommendations = await run_in_threadpool(recommend, recommender, payload)
    return RecommendationResponse(
        version=recommender.model_version,
        items=tuple(
            RecommendationItem(game_id=item.game_id, score=item.score)
            for item in recommendations
        ),
    )
