"""Persistance atomique et chargement contrôlé des artefacts KNN."""

import os
import tempfile
from datetime import datetime
from enum import Enum
from pathlib import Path

import joblib
from scipy.sparse import isspmatrix_csr
from sklearn import __version__ as sklearn_version
from sklearn.neighbors import NearestNeighbors

from gamesup_api.model.recommender import (
    ARTIFACT_SCHEMA_VERSION,
    KnnRecommender,
    RecommenderArtifact,
)
from gamesup_api.schemas import PopularityScore, PreparationStatistics


class ModelArtifactError(RuntimeError):
    """Erreur de chargement d'un artefact de recommandation."""


class ModelArtifactNotFoundError(ModelArtifactError):
    """Aucun artefact n'a encore été entraîné."""


class InvalidModelArtifactError(ModelArtifactError):
    """L'artefact est illisible ou structurellement invalide."""


class IncompatibleModelArtifactError(ModelArtifactError):
    """L'artefact utilise une version de format ou de bibliothèque différente."""


class ModelStatus(str, Enum):
    """État de chargement du modèle dans le processus FastAPI."""

    UNINITIALIZED = "UNINITIALIZED"
    READY = "READY"
    NOT_TRAINED = "NOT_TRAINED"
    INVALID = "INVALID"
    INCOMPATIBLE = "INCOMPATIBLE"


def save_recommender(recommender: KnnRecommender, path: Path) -> Path:
    """Écrit l'artefact dans le même dossier puis le remplace atomiquement."""

    destination = Path(path)
    destination.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(
        dir=destination.parent,
        prefix=f".{destination.name}.",
        suffix=".tmp",
    )
    os.close(descriptor)
    temporary_path = Path(temporary_name)

    try:
        joblib.dump(recommender.artifact, temporary_path, compress=3)
        os.replace(temporary_path, destination)
    except Exception:
        temporary_path.unlink(missing_ok=True)
        raise

    return destination


def load_recommender(path: Path) -> KnnRecommender:
    """Charge un artefact interne approuvé et vérifie sa compatibilité."""

    source = Path(path)
    if not source.is_file():
        raise ModelArtifactNotFoundError("recommendation model artifact is absent")

    try:
        artifact = joblib.load(source)
    except Exception as error:
        raise InvalidModelArtifactError(
            "recommendation model artifact cannot be read"
        ) from error

    if not isinstance(artifact, RecommenderArtifact):
        raise InvalidModelArtifactError(
            "recommendation model artifact has an unexpected type"
        )
    _validate_compatibility(artifact)
    try:
        _validate_structure(artifact)
    except InvalidModelArtifactError:
        raise
    except Exception as error:
        raise InvalidModelArtifactError(
            "recommendation model artifact structure is invalid"
        ) from error
    return KnnRecommender(artifact)


def _validate_compatibility(artifact: RecommenderArtifact) -> None:
    if artifact.artifact_schema_version != ARTIFACT_SCHEMA_VERSION:
        raise IncompatibleModelArtifactError(
            "recommendation model artifact format is incompatible"
        )
    if artifact.sklearn_version != sklearn_version:
        raise IncompatibleModelArtifactError(
            "recommendation model scikit-learn version is incompatible"
        )


def _validate_structure(artifact: RecommenderArtifact) -> None:
    game_count = len(artifact.game_id_by_index)
    user_count = len(artifact.user_id_by_index)
    expected_users = {
        user_id: index
        for index, user_id in enumerate(artifact.user_id_by_index)
    }
    expected_games = {
        game_id: index
        for index, game_id in enumerate(artifact.game_id_by_index)
    }
    popularity_ids = [item.game_id for item in artifact.popularity]

    is_valid = (
        isinstance(artifact.model_version, str)
        and artifact.model_version.startswith("knn-")
        and isinstance(artifact.trained_at, datetime)
        and artifact.trained_at.tzinfo is not None
        and isinstance(artifact.model, NearestNeighbors)
        and artifact.model.metric == "cosine"
        and artifact.model.algorithm == "brute"
        and isspmatrix_csr(artifact.item_user_matrix)
        and artifact.item_user_matrix.shape == (game_count, user_count)
        and artifact.user_to_index == expected_users
        and artifact.game_to_index == expected_games
        and 1 <= artifact.neighbor_count <= game_count
        and artifact.model.n_neighbors == artifact.neighbor_count
        and getattr(artifact.model, "_fit_X", None) is not None
        and artifact.model._fit_X.shape == artifact.item_user_matrix.shape
        and isinstance(artifact.statistics, PreparationStatistics)
        and all(
            isinstance(item, PopularityScore) for item in artifact.popularity
        )
        and len(popularity_ids) == game_count
        and set(popularity_ids) == set(artifact.game_id_by_index)
    )
    if not is_valid:
        raise InvalidModelArtifactError(
            "recommendation model artifact structure is invalid"
        )
