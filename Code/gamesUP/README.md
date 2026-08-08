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
