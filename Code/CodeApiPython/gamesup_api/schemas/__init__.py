"""Schémas d'entrée et de sortie de l'API."""

from gamesup_api.schemas.training import (
    PopularityScore,
    PreparationStatistics,
    TrainingBatch,
    TrainingInteraction,
)
from gamesup_api.schemas.recommendations import (
    RecommendationHistoryItem,
    RecommendationItem,
    RecommendationRequest,
    RecommendationResponse,
    TrainModelResponse,
)

__all__ = [
    "PopularityScore",
    "PreparationStatistics",
    "RecommendationHistoryItem",
    "RecommendationItem",
    "RecommendationRequest",
    "RecommendationResponse",
    "TrainModelResponse",
    "TrainingBatch",
    "TrainingInteraction",
]
