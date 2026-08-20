"""Schémas de la route de supervision."""

from typing import Literal

from pydantic import BaseModel


class HealthResponse(BaseModel):
    """État public minimal du service."""

    status: Literal["UP"]
    version: str
