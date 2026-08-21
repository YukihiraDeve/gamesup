#!/bin/sh

set -eu

gamesup_url=${GAMESUP_URL:-http://localhost:${SPRING_PORT:-8080}}
demo_email="docker-demo-$(date +%s)-$$@example.test"

registration_response=$(curl --fail --silent --show-error \
	--request POST \
	--header 'Content-Type: application/json' \
	--data "{\"email\":\"${demo_email}\",\"password\":\"Docker-demo-password-2026!\",\"firstName\":\"Docker\",\"lastName\":\"Demo\"}" \
	"${gamesup_url}/api/v1/auth/register")

access_token=$(printf '%s' "$registration_response" \
	| sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

if [ -z "$access_token" ]; then
	echo "La réponse d'inscription ne contient pas de jeton." >&2
	exit 1
fi

curl --fail --silent --show-error \
	--header "Authorization: Bearer ${access_token}" \
	"${gamesup_url}/api/v1/users/me"
printf '\n'
