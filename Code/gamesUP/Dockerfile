# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src/ src/
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
	&& apt-get install --yes --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/* \
	&& groupadd --gid 10001 gamesup \
	&& useradd --uid 10001 --gid gamesup --no-create-home --shell /usr/sbin/nologin gamesup

WORKDIR /app

COPY --from=build --chown=gamesup:gamesup /workspace/target/gamesup-api-*.jar /app/gamesup-api.jar

USER gamesup:gamesup

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/gamesup-api.jar"]
