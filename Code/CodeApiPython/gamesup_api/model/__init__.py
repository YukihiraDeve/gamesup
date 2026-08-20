"""Modèle KNN et persistance des artefacts de recommandation."""

from gamesup_api.model.recommender import (
    KnnRecommender,
    Recommendation,
    RecommenderArtifact,
    train_recommender,
)
from gamesup_api.model.storage import (
    IncompatibleModelArtifactError,
    InvalidModelArtifactError,
    ModelArtifactNotFoundError,
    ModelStatus,
    load_recommender,
    save_recommender,
)

__all__ = [
    "IncompatibleModelArtifactError",
    "InvalidModelArtifactError",
    "KnnRecommender",
    "ModelArtifactNotFoundError",
    "ModelStatus",
    "Recommendation",
    "RecommenderArtifact",
    "load_recommender",
    "save_recommender",
    "train_recommender",
]
