"""Préparation déterministe de la matrice utilisateur-jeu."""

from collections import defaultdict
from dataclasses import dataclass
from math import fsum
from statistics import median
from typing import Mapping

from scipy.sparse import csr_matrix

from gamesup_api.schemas.training import (
    PopularityScore,
    PreparationStatistics,
    TrainingBatch,
    TrainingInteraction,
)

IMPLICIT_RATING = 3.0
MINIMUM_USERS = 2
MINIMUM_GAMES = 2
MINIMUM_INTERACTIONS = 2


class InsufficientTrainingDataError(ValueError):
    """Indique qu'un lot ne permet pas de préparer un modèle exploitable."""


@dataclass(frozen=True, slots=True)
class PreparedTrainingData:
    """Données déterministes prêtes pour l'entraînement du modèle."""

    user_game_matrix: csr_matrix
    user_to_index: Mapping[int, int]
    game_to_index: Mapping[int, int]
    user_id_by_index: tuple[int, ...]
    game_id_by_index: tuple[int, ...]
    popularity: tuple[PopularityScore, ...]
    statistics: PreparationStatistics


@dataclass(frozen=True, slots=True)
class _NormalizedInteraction:
    user_id: int
    game_id: int
    rating: float
    is_explicit: bool


def prepare_training_data(batch: TrainingBatch) -> PreparedTrainingData:
    """Valide et transforme un lot anonymisé en matrice sparse stable."""

    normalized = _deduplicate_interactions(batch.interactions)
    user_ids = tuple(sorted({item.user_id for item in normalized}))
    game_ids = tuple(sorted({item.game_id for item in normalized}))
    _validate_minimum_dataset(normalized, user_ids, game_ids)

    user_to_index = {user_id: index for index, user_id in enumerate(user_ids)}
    game_to_index = {game_id: index for index, game_id in enumerate(game_ids)}
    matrix = _build_matrix(normalized, user_to_index, game_to_index)
    popularity, global_mean, prior_weight = _compute_popularity(
        normalized, game_ids
    )

    explicit_ratings = sum(item.is_explicit for item in normalized)
    statistics = PreparationStatistics(
        received_interactions=len(batch.interactions),
        retained_interactions=len(normalized),
        duplicates_removed=len(batch.interactions) - len(normalized),
        explicit_ratings=explicit_ratings,
        implicit_ratings=len(normalized) - explicit_ratings,
        users=len(user_ids),
        games=len(game_ids),
        global_mean_rating=global_mean,
        popularity_prior_weight=prior_weight,
    )

    return PreparedTrainingData(
        user_game_matrix=matrix,
        user_to_index=dict(user_to_index),
        game_to_index=dict(game_to_index),
        user_id_by_index=user_ids,
        game_id_by_index=game_ids,
        popularity=popularity,
        statistics=statistics,
    )


def _deduplicate_interactions(
    interactions: tuple[TrainingInteraction, ...],
) -> tuple[_NormalizedInteraction, ...]:
    grouped: dict[tuple[int, int], list[float]] = defaultdict(list)
    all_keys: set[tuple[int, int]] = set()

    for interaction in interactions:
        key = (interaction.user_id, interaction.game_id)
        all_keys.add(key)
        if interaction.rating is not None:
            grouped[key].append(float(interaction.rating))

    normalized: list[_NormalizedInteraction] = []
    for user_id, game_id in sorted(all_keys):
        explicit_ratings = sorted(grouped[(user_id, game_id)])
        is_explicit = bool(explicit_ratings)
        rating = (
            fsum(explicit_ratings) / len(explicit_ratings)
            if is_explicit
            else IMPLICIT_RATING
        )
        normalized.append(
            _NormalizedInteraction(
                user_id=user_id,
                game_id=game_id,
                rating=rating,
                is_explicit=is_explicit,
            )
        )

    return tuple(normalized)


def _validate_minimum_dataset(
    interactions: tuple[_NormalizedInteraction, ...],
    user_ids: tuple[int, ...],
    game_ids: tuple[int, ...],
) -> None:
    if (
        len(interactions) < MINIMUM_INTERACTIONS
        or len(user_ids) < MINIMUM_USERS
        or len(game_ids) < MINIMUM_GAMES
    ):
        raise InsufficientTrainingDataError(
            "training data must contain at least two users, two games "
            "and two usable interactions"
        )


def _build_matrix(
    interactions: tuple[_NormalizedInteraction, ...],
    user_to_index: Mapping[int, int],
    game_to_index: Mapping[int, int],
) -> csr_matrix:
    rows = [user_to_index[item.user_id] for item in interactions]
    columns = [game_to_index[item.game_id] for item in interactions]
    ratings = [item.rating for item in interactions]
    matrix = csr_matrix(
        (ratings, (rows, columns)),
        shape=(len(user_to_index), len(game_to_index)),
        dtype=float,
    )
    matrix.sort_indices()
    return matrix


def _compute_popularity(
    interactions: tuple[_NormalizedInteraction, ...],
    game_ids: tuple[int, ...],
) -> tuple[tuple[PopularityScore, ...], float, float]:
    """Ramène chaque moyenne vers la moyenne globale selon le volume médian."""

    ratings_by_game: dict[int, list[float]] = defaultdict(list)
    for interaction in interactions:
        ratings_by_game[interaction.game_id].append(interaction.rating)

    all_ratings = [item.rating for item in interactions]
    global_mean = fsum(all_ratings) / len(all_ratings)
    prior_weight = float(
        median(len(ratings_by_game[game_id]) for game_id in game_ids)
    )

    scores: list[PopularityScore] = []
    for game_id in game_ids:
        ratings = ratings_by_game[game_id]
        rating_count = len(ratings)
        average_rating = fsum(ratings) / rating_count
        weighted_score = (
            rating_count * average_rating + prior_weight * global_mean
        ) / (rating_count + prior_weight)
        scores.append(
            PopularityScore(
                game_id=game_id,
                average_rating=average_rating,
                weighted_score=weighted_score,
                rating_count=rating_count,
            )
        )

    scores.sort(
        key=lambda item: (-item.weighted_score, -item.rating_count, item.game_id)
    )
    return tuple(scores), global_mean, prior_weight
