# API Java GamesUP

## Prérequis

- un JDK 21 ;
- aucune installation globale de Maven : le wrapper fourni télécharge Maven 3.9.9.

Le JDK Temurin 21 peut être téléchargé depuis [Adoptium](https://adoptium.net/temurin/releases/?version=21). Après son installation, vérifier la version active :

```bash
java -version
```

Sous macOS, si plusieurs JDK sont installés, sélectionner Java 21 pour le terminal courant :

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
```

## Compiler

Depuis ce dossier :

```bash
./mvnw -version
./mvnw -q -DskipTests compile
```

Le projet cible explicitement Java 21. Les versions des dépendances Spring sont gérées par Spring Boot 3.3.4 ; les autres versions nécessaires au build sont déclarées dans `pom.xml`.

## Configuration locale

Le profil `local` est actif par défaut. Définir les paramètres de connexion avant de démarrer l'API :

```bash
export DATABASE_URL="<URL JDBC MySQL locale>"
export DATABASE_USERNAME="<utilisateur MySQL local>"
export DATABASE_PASSWORD="<mot de passe MySQL local>"
export CORS_ALLOWED_ORIGINS="http://localhost:4200"
export JWT_SECRET="<secret JWT local>"
export FASTAPI_URL="http://localhost:8000"
export FASTAPI_SERVICE_KEY="<clé partagée d'au moins 16 caractères>"
export FASTAPI_CONNECT_TIMEOUT="PT1S"
export FASTAPI_READ_TIMEOUT="PT3S"
./mvnw spring-boot:run
```

Ces valeurs sont uniquement des exemples locaux. La même clé doit être fournie au service Python via
`SERVICE_API_KEY`. Aucun identifiant ni secret réel ne doit être ajouté au dépôt.

Le profil `test` utilise une base H2 en mémoire indépendante de MySQL :

```bash
./mvnw test
```
