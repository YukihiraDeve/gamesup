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

L'état du service est disponible sur `http://127.0.0.1:8000/health`. Cette étape n'expose volontairement aucune route de recommandation.

## Vérification syntaxique

Avec l'environnement virtuel activé :

```bash
python -m compileall gamesup_api
```
