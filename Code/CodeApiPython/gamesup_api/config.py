"""Configuration externalisée du service."""

from functools import lru_cache
from ipaddress import IPv4Network, IPv6Network, ip_network
from pathlib import Path

from pydantic import Field, SecretStr, field_validator, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

ALLOWED_INTERNAL_RANGES = tuple(
    ip_network(cidr)
    for cidr in (
        "10.0.0.0/8",
        "127.0.0.0/8",
        "169.254.0.0/16",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "::1/128",
        "fc00::/7",
        "fe80::/10",
    )
)


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
    internal_api_cidrs: str = "127.0.0.0/8,::1/128"
    service_api_key: SecretStr | None = None

    @field_validator("app_env")
    @classmethod
    def normalize_app_env(cls, value: str) -> str:
        """Normalise l'environnement avant les contrôles de sécurité."""

        normalized = value.strip().lower()
        if not normalized:
            raise ValueError("application environment cannot be empty")
        return normalized

    @field_validator("internal_api_cidrs")
    @classmethod
    def validate_internal_api_cidrs(cls, value: str) -> str:
        """Normalise et valide la liste de réseaux autorisés."""

        cidrs = [cidr.strip() for cidr in value.split(",") if cidr.strip()]
        if not cidrs:
            raise ValueError("at least one internal API network is required")
        networks = [ip_network(cidr, strict=False) for cidr in cidrs]
        if any(
            not any(
                network.version == allowed_range.version
                and network.subnet_of(allowed_range)
                for allowed_range in ALLOWED_INTERNAL_RANGES
            )
            for network in networks
        ):
            raise ValueError("internal API networks cannot be publicly routable")
        return ",".join(str(network) for network in networks)

    @field_validator("service_api_key")
    @classmethod
    def validate_service_api_key(
        cls, value: SecretStr | None
    ) -> SecretStr | None:
        """Refuse une clé configurée mais vide ou trop faible."""

        if value is None:
            return None
        secret = value.get_secret_value()
        if len(secret) < 16 or not secret.strip():
            raise ValueError("service API key must contain at least 16 characters")
        return value

    @model_validator(mode="after")
    def require_production_service_api_key(self) -> "Settings":
        """Interdit tout démarrage de production sans clé explicite."""

        if self.app_env in {"prod", "production"}:
            if self.service_api_key is None:
                raise ValueError("service API key is required in production")
        return self

    def allowed_internal_networks(
        self,
    ) -> tuple[IPv4Network | IPv6Network, ...]:
        """Retourne les réseaux internes déjà validés."""

        return tuple(
            ip_network(cidr, strict=False)
            for cidr in self.internal_api_cidrs.split(",")
        )


@lru_cache
def get_settings() -> Settings:
    """Retourne une configuration validée et partagée par le processus."""

    return Settings()
