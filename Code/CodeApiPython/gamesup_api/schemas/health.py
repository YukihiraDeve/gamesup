"""Schémas de la route de supervision."""

from typing import Literal

from pydantic import BaseModel, ConfigDict


class HealthResponse(BaseModel):
    """État public minimal du service."""

    model_config = ConfigDict(protected_namespaces=())

    status: Literal["UP"]
    version: str
    model_status: Literal[
        "UNINITIALIZED",
        "READY",
        "NOT_TRAINED",
        "INVALID",
        "INCOMPATIBLE",
    ]
