"""Contrats des données utilisées pour préparer l'entraînement."""

from typing import Annotated

from pydantic import BaseModel, ConfigDict, Field

PositiveIdentifier = Annotated[int, Field(strict=True, gt=0)]
NonNegativeInteger = Annotated[int, Field(ge=0)]
BoundedRating = Annotated[float, Field(strict=True, ge=1.0, le=5.0)]


class StrictSchema(BaseModel):
    """Base immuable qui refuse tout champ non prévu, notamment les PII."""

    model_config = ConfigDict(extra="forbid", frozen=True)


class TrainingInteraction(StrictSchema):
    """Achat d'un jeu, avec une note explicite optionnelle."""

    user_id: PositiveIdentifier
    game_id: PositiveIdentifier
    rating: BoundedRating | None = None


class TrainingBatch(StrictSchema):
    """Lot d'achats anonymisés ; la wishlist est absente du contrat v1."""

    interactions: tuple[TrainingInteraction, ...] = Field(min_length=1)


class PreparationStatistics(StrictSchema):
    """Mesures décrivant les transformations appliquées au lot."""

    received_interactions: NonNegativeInteger
    retained_interactions: NonNegativeInteger
    duplicates_removed: NonNegativeInteger
    explicit_ratings: NonNegativeInteger
    implicit_ratings: NonNegativeInteger
    users: NonNegativeInteger
    games: NonNegativeInteger
    global_mean_rating: BoundedRating
    popularity_prior_weight: Annotated[float, Field(gt=0.0)]


class PopularityScore(StrictSchema):
    """Score bayésien stable utilisé uniquement pour le démarrage à froid."""

    game_id: PositiveIdentifier
    average_rating: BoundedRating
    weighted_score: BoundedRating
    rating_count: Annotated[int, Field(gt=0)]
