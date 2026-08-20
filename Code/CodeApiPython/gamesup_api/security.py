"""Contrôle de l'accès au service interne."""

from ipaddress import IPv4Address, IPv6Address, ip_address
from secrets import compare_digest
from typing import Annotated

from fastapi import HTTPException, Request, Security, status
from fastapi.security import APIKeyHeader

service_key_header = APIKeyHeader(
    name="X-Service-Key",
    auto_error=False,
    description="Clé partagée entre Spring et le service de recommandation.",
)


async def require_internal_service(
    request: Request,
    provided_key: Annotated[str | None, Security(service_key_header)],
) -> None:
    """Autorise seulement un client du réseau interne avec la bonne clé."""

    client_host = request.client.host if request.client is not None else None
    if not _is_internal_client(client_host, request):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="client network is not allowed",
        )

    configured_key = request.app.state.settings.service_api_key
    if configured_key is None:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="service authentication is not configured",
        )
    expected_key = configured_key.get_secret_value()
    if provided_key is None or not compare_digest(provided_key, expected_key):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="invalid service credentials",
        )


def _is_internal_client(client_host: str | None, request: Request) -> bool:
    if client_host is None:
        return False
    try:
        address = ip_address(client_host)
    except ValueError:
        return False

    comparable_address: IPv4Address | IPv6Address = address
    if isinstance(address, IPv6Address) and address.ipv4_mapped is not None:
        comparable_address = address.ipv4_mapped
    return any(
        comparable_address in network
        for network in request.app.state.settings.allowed_internal_networks()
        if comparable_address.version == network.version
    )
