# syntax=docker/dockerfile:1

FROM python:3.11.10-slim-bookworm AS build

ENV PIP_DISABLE_PIP_VERSION_CHECK=1 \
	PIP_NO_CACHE_DIR=1 \
	PATH="/opt/venv/bin:$PATH"

WORKDIR /workspace

RUN python -m venv /opt/venv

COPY pyproject.toml readme.md ./
COPY gamesup_api/ gamesup_api/
RUN python -m pip install --upgrade "pip==24.3.1" \
	&& python -m pip install . \
	&& python -m pip check

FROM python:3.11.10-slim-bookworm AS runtime

ENV APP_ENV=development \
	APP_HOST=0.0.0.0 \
	APP_PORT=8000 \
	MODEL_ARTIFACT_PATH=/app/artifacts/recommendation.joblib \
	PATH="/opt/venv/bin:$PATH" \
	PYTHONDONTWRITEBYTECODE=1 \
	PYTHONUNBUFFERED=1

RUN groupadd --gid 10001 gamesup \
	&& useradd --uid 10001 --gid gamesup --no-create-home --shell /usr/sbin/nologin gamesup \
	&& mkdir --parents /app/artifacts \
	&& chown --recursive gamesup:gamesup /app

COPY --from=build /opt/venv/ /opt/venv/

WORKDIR /app

USER gamesup:gamesup

EXPOSE 8000

CMD ["uvicorn", "gamesup_api.main:app", "--host", "0.0.0.0", "--port", "8000"]
