# Exécution locale avec Docker

La plateforme conteneurisée comprend MySQL, l'API FastAPI interne et l'API Spring publique.
Java, Python et MySQL ne sont pas requis sur la machine hôte.

## Préparer l'environnement

Depuis la racine du dépôt :

```bash
cp .env.example .env
```

Les valeurs du fichier d'exemple sont réservées au développement. Remplacer au minimum
`MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `JWT_SECRET` et `FASTAPI_SERVICE_KEY` avant tout usage partagé.
Le fichier `.env` est ignoré par Git.

## Construire et démarrer

```bash
docker compose build --pull --no-cache
docker compose up --detach
docker compose ps
```

Spring est le seul service publié sur la machine hôte, par défaut sur
`http://localhost:8080`. MySQL et FastAPI restent joignables uniquement sur le réseau Docker interne.
Spring attend que les contrôles de santé MySQL et FastAPI réussissent avant de démarrer. Flyway applique
ensuite automatiquement les migrations de la base.

## Vérifier la plateforme

```bash
curl --fail 'http://localhost:8080/api/v1/games?size=1'
docker compose exec fastapi python -c "import urllib.request; print(urllib.request.urlopen('http://127.0.0.1:8000/health').read().decode())"
docker compose exec mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank"'
```

Le parcours de démonstration suivant crée un compte local éphémère, récupère son JWT puis consulte son
profil authentifié. Il n'est jamais lancé automatiquement et ne doit pas être utilisé en production :

```bash
./scripts/demo.sh
```

## Arrêter

```bash
docker compose down
```

Cette commande conserve les volumes `mysql_data` et `knn_artifacts`. La variante
`docker compose down --volumes` supprime définitivement les données locales et le modèle entraîné.
