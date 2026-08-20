"""Orchestration synchrone des opérations de recommandation."""

from pathlib import Path

from gamesup_api.data import prepare_training_data
from gamesup_api.model import (
    KnnRecommender,
    Recommendation,
    save_recommender,
    train_recommender,
)
from gamesup_api.schemas import RecommendationRequest, TrainingBatch


def train_and_persist(
    batch: TrainingBatch,
    neighbor_count: int,
    artifact_path: Path,
) -> KnnRecommender:
    """Prépare, entraîne et publie atomiquement un nouveau modèle."""

    prepared = prepare_training_data(batch)
    recommender = train_recommender(prepared, neighbor_count)
    save_recommender(recommender, artifact_path)
    return recommender


def recommend(
    recommender: KnnRecommender,
    request: RecommendationRequest,
) -> tuple[Recommendation, ...]:
    """Adapte le contrat HTTP au modèle sans transmettre de PII."""

    current_ratings = {
        item.game_id: item.rating for item in request.history
    }
    return recommender.recommend(
        user_id=request.user_id,
        limit=request.limit,
        current_ratings=current_ratings,
    )
