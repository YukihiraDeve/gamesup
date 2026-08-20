"""Contrats HTTP d'entraînement et de recommandation."""

from typing import Annotated

from pydantic import BaseModel, ConfigDict, Field, field_validator

PositiveIdentifier = Annotated[int, Field(strict=True, gt=0)]
BoundedRating = Annotated[float, Field(strict=True, ge=1.0, le=5.0)]


class StrictHttpSchema(BaseModel):
    """Contrat immuable qui refuse les champs inconnus, notamment les PII."""

    model_config = ConfigDict(extra="forbid", frozen=True)


class RecommendationHistoryItem(StrictHttpSchema):
    """Jeu connu de l'utilisateur, avec note explicite optionnelle."""

    game_id: PositiveIdentifier
    rating: BoundedRating | None = None


class RecommendationRequest(StrictHttpSchema):
    """Demande anonymisée et bornée de recommandations."""

    user_id: PositiveIdentifier
    history: tuple[RecommendationHistoryItem, ...] = ()
    limit: Annotated[int, Field(strict=True, ge=1, le=100)] = 10

    @field_validator("history")
    @classmethod
    def reject_duplicate_games(
        cls,
        value: tuple[RecommendationHistoryItem, ...],
    ) -> tuple[RecommendationHistoryItem, ...]:
        """Refuse un historique ambigu pour garantir un résultat stable."""

        game_ids = [item.game_id for item in value]
        if len(game_ids) != len(set(game_ids)):
            raise ValueError("history must contain unique game identifiers")
        return value


class TrainModelResponse(StrictHttpSchema):
    """Résumé stable du modèle entraîné."""

    version: str
    users: Annotated[int, Field(ge=2)]
    games: Annotated[int, Field(ge=2)]
    retained_interactions: Annotated[int, Field(ge=2)]


class RecommendationItem(StrictHttpSchema):
    """Jeu recommandé et score normalisé."""

    game_id: PositiveIdentifier
    score: Annotated[float, Field(ge=0.0, le=1.0)]


class RecommendationResponse(StrictHttpSchema):
    """Résultats ordonnés associés à une version du modèle."""

    version: str
    items: tuple[RecommendationItem, ...]
