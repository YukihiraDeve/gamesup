# API de recommandation GamesUP

## Prérequis

- Python 3.10, 3.11 ou 3.12 ;
- `venv` et `pip` fournis avec Python.

## Installation

Depuis ce dossier, créer un environnement virtuel isolé puis installer le projet en mode éditable :

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade "pip==24.3.1"
python -m pip install --editable .
```

Les dépendances directes sont épinglées dans `pyproject.toml`.

## Configuration et démarrage

La configuration locale d'exemple ne contient aucun secret :

```bash
cp .env.example .env
set -a
source .env
set +a
uvicorn gamesup_api.main:app --host "$APP_HOST" --port "$APP_PORT" --reload
```

`SERVICE_API_KEY` doit contenir une valeur secrète d'au moins 16 caractères et ne doit jamais être commitée. `INTERNAL_API_CIDRS` contient la liste séparée par des virgules des réseaux autorisés à appeler les routes internes. En production, le service refuse de démarrer sans clé explicite.

## Contrat HTTP interne

- `GET /health` reste disponible même si aucun modèle n'est chargé ;
- `POST /model/train` entraîne et persiste le modèle hors du thread événementiel ;
- `POST /recommendations` retourne une version de modèle et des items `{game_id, score}`.

Les deux routes `POST` exigent l'en-tête `X-Service-Key` et une adresse cliente appartenant à `INTERNAL_API_CIDRS`. Les corps n'acceptent que des identifiants techniques et des notes, jamais de nom ni d'adresse électronique.

## Vérification syntaxique

Avec l'environnement virtuel activé :

```bash
python -m compileall gamesup_api
```
