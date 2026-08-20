"""Entraînement et utilisation du modèle KNN item-item."""

from collections import defaultdict
from dataclasses import dataclass
from datetime import datetime, timezone
from hashlib import sha256
from json import dumps
from math import fsum, isfinite
from typing import Mapping

from scipy.sparse import csr_matrix
from sklearn import __version__ as sklearn_version
from sklearn.neighbors import NearestNeighbors

from gamesup_api.data import IMPLICIT_RATING, PreparedTrainingData
from gamesup_api.schemas import PopularityScore, PreparationStatistics

ARTIFACT_SCHEMA_VERSION = 1
LIKED_RATING_THRESHOLD = 3.0


@dataclass(frozen=True, slots=True)
class Recommendation:
    """Recommandation calculée et son score normalisé."""

    game_id: int
    score: float


@dataclass(frozen=True)
class RecommenderArtifact:
    """État complet et versionné nécessaire aux recommandations."""

    artifact_schema_version: int
    model_version: str
    trained_at: datetime
    sklearn_version: str
    neighbor_count: int
    model: NearestNeighbors
    item_user_matrix: csr_matrix
    user_to_index: dict[int, int]
    game_to_index: dict[int, int]
    user_id_by_index: tuple[int, ...]
    game_id_by_index: tuple[int, ...]
    popularity: tuple[PopularityScore, ...]
    statistics: PreparationStatistics


class KnnRecommender:
    """Façade de recommandation autour d'un artefact entraîné."""

    def __init__(self, artifact: RecommenderArtifact) -> None:
        self._artifact = artifact

    @property
    def artifact(self) -> RecommenderArtifact:
        return self._artifact

    @property
    def model_version(self) -> str:
        return self._artifact.model_version

    def recommend(
        self,
        user_id: int,
        limit: int = 10,
        current_ratings: Mapping[int, float | None] | None = None,
    ) -> tuple[Recommendation, ...]:
        """Recommande des jeux inconnus ou utilise la popularité en repli."""

        _validate_positive_integer(user_id, "user_id")
        _validate_positive_integer(limit, "limit")

        is_known_user = user_id in self._artifact.user_to_index
        ratings = self._stored_ratings(user_id) if is_known_user else {}
        ratings.update(_validate_current_ratings(current_ratings or {}))
        known_game_ids = set(ratings)

        if not is_known_user:
            return self._fill_with_popularity((), known_game_ids, limit)

        liked_ratings = {
            game_id: rating
            for game_id, rating in ratings.items()
            if rating > LIKED_RATING_THRESHOLD
            and game_id in self._artifact.game_to_index
        }
        calculated = self._calculate_neighbor_scores(
            liked_ratings, known_game_ids
        )
        return self._fill_with_popularity(calculated, known_game_ids, limit)

    def _stored_ratings(self, user_id: int) -> dict[int, float]:
        user_index = self._artifact.user_to_index[user_id]
        user_column = self._artifact.item_user_matrix.getcol(user_index).tocoo()
        return {
            self._artifact.game_id_by_index[game_index]: float(rating)
            for game_index, rating in zip(user_column.row, user_column.data)
        }

    def _calculate_neighbor_scores(
        self,
        liked_ratings: Mapping[int, float],
        known_game_ids: set[int],
    ) -> tuple[Recommendation, ...]:
        if not liked_ratings:
            return ()

        total_preference = fsum(
            _normalize_preference(rating) for rating in liked_ratings.values()
        )
        scores: dict[int, float] = defaultdict(float)

        for game_id, rating in sorted(liked_ratings.items()):
            game_index = self._artifact.game_to_index[game_id]
            distances, neighbor_indices = self._artifact.model.kneighbors(
                self._artifact.item_user_matrix.getrow(game_index),
                n_neighbors=self._artifact.neighbor_count,
                return_distance=True,
            )
            preference = _normalize_preference(rating)
            for distance, neighbor_index in zip(
                distances[0], neighbor_indices[0]
            ):
                neighbor_game_id = self._artifact.game_id_by_index[
                    int(neighbor_index)
                ]
                if neighbor_game_id in known_game_ids:
                    continue
                similarity = min(1.0, max(0.0, 1.0 - float(distance)))
                if similarity > 0.0:
                    scores[neighbor_game_id] += similarity * preference

        recommendations = (
            Recommendation(game_id=game_id, score=score / total_preference)
            for game_id, score in scores.items()
        )
        return tuple(
            sorted(recommendations, key=lambda item: (-item.score, item.game_id))
        )

    def _fill_with_popularity(
        self,
        calculated: tuple[Recommendation, ...],
        known_game_ids: set[int],
        limit: int,
    ) -> tuple[Recommendation, ...]:
        selected = list(calculated[:limit])
        selected_ids = {item.game_id for item in selected}
        fallback_scale = selected[-1].score if selected else 1.0

        for popular_game in self._artifact.popularity:
            if len(selected) >= limit:
                break
            if (
                popular_game.game_id in known_game_ids
                or popular_game.game_id in selected_ids
            ):
                continue
            selected.append(
                Recommendation(
                    game_id=popular_game.game_id,
                    score=(
                        _normalize_popularity(popular_game.weighted_score)
                        * fallback_scale
                    ),
                )
            )
            selected_ids.add(popular_game.game_id)

        return tuple(selected)


def train_recommender(
    prepared: PreparedTrainingData,
    neighbor_count: int,
) -> KnnRecommender:
    """Entraîne un KNN cosinus brute-force compatible avec les matrices sparse."""

    _validate_positive_integer(neighbor_count, "neighbor_count")
    item_user_matrix = prepared.user_game_matrix.transpose().tocsr()
    effective_neighbor_count = min(neighbor_count, item_user_matrix.shape[0])
    model = NearestNeighbors(
        n_neighbors=effective_neighbor_count,
        metric="cosine",
        algorithm="brute",
    )
    model.fit(item_user_matrix)

    artifact = RecommenderArtifact(
        artifact_schema_version=ARTIFACT_SCHEMA_VERSION,
        model_version=_build_model_version(
            prepared, effective_neighbor_count
        ),
        trained_at=datetime.now(timezone.utc),
        sklearn_version=sklearn_version,
        neighbor_count=effective_neighbor_count,
        model=model,
        item_user_matrix=item_user_matrix,
        user_to_index=dict(prepared.user_to_index),
        game_to_index=dict(prepared.game_to_index),
        user_id_by_index=prepared.user_id_by_index,
        game_id_by_index=prepared.game_id_by_index,
        popularity=prepared.popularity,
        statistics=prepared.statistics,
    )
    return KnnRecommender(artifact)


def _validate_current_ratings(
    ratings: Mapping[int, float | None],
) -> dict[int, float]:
    validated: dict[int, float] = {}
    for game_id, rating in ratings.items():
        _validate_positive_integer(game_id, "game_id")
        if rating is None:
            validated[game_id] = IMPLICIT_RATING
            continue
        if (
            isinstance(rating, bool)
            or not isinstance(rating, (int, float))
            or not isfinite(rating)
            or not 1.0 <= rating <= 5.0
        ):
            raise ValueError("rating must be a finite number between 1 and 5")
        validated[game_id] = float(rating)
    return validated


def _validate_positive_integer(value: int, name: str) -> None:
    if isinstance(value, bool) or not isinstance(value, int) or value <= 0:
        raise ValueError(f"{name} must be a positive integer")


def _normalize_preference(rating: float) -> float:
    return (rating - LIKED_RATING_THRESHOLD) / (
        5.0 - LIKED_RATING_THRESHOLD
    )


def _normalize_popularity(weighted_score: float) -> float:
    return (weighted_score - 1.0) / 4.0


def _build_model_version(
    prepared: PreparedTrainingData,
    neighbor_count: int,
) -> str:
    matrix = prepared.user_game_matrix
    fingerprint = dumps(
        {
            "artifact_schema_version": ARTIFACT_SCHEMA_VERSION,
            "algorithm": "brute",
            "metric": "cosine",
            "neighbor_count": neighbor_count,
            "sklearn_version": sklearn_version,
            "user_ids": prepared.user_id_by_index,
            "game_ids": prepared.game_id_by_index,
            "indptr": matrix.indptr.tolist(),
            "indices": matrix.indices.tolist(),
            "ratings": matrix.data.tolist(),
        },
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return f"knn-{sha256(fingerprint).hexdigest()[:16]}"
