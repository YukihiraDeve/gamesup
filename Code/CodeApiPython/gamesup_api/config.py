"""Configuration externalisée du service."""

from functools import lru_cache
from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Paramètres applicatifs lus depuis l'environnement ou un fichier .env."""

    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore",
        protected_namespaces=("settings_",),
    )

    app_env: str = "development"
    app_host: str = "127.0.0.1"
    app_port: int = Field(default=8000, ge=1, le=65535)
    model_artifact_path: Path = Path("artifacts/recommendation.joblib")
    knn_neighbors: int = Field(default=5, ge=1)


@lru_cache
def get_settings() -> Settings:
    """Retourne une configuration validée et partagée par le processus."""

    return Settings()
