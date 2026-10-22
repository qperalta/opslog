#!/usr/bin/env bash
# Builds, tests, and deploys OpsLog: Maven test -> Docker img -> Docker hub -> Render
set -euo pipefail

cd "$(dirname "$0")/.."

# let's load local settings: (.env.example for var names):
if [[-f .env]]; then
  set -a; source .env; set +a
fi
: "${DOCKER_IMAGE:?DOCKER_IMAGE is not set (see .env.example)}"
: "${RENDER_DEPLOY_HOOK_URL:?RENDER_DEPLOY_HOOK_URL is not set (see .env.example)}"

# let's make sure to only deploy committed codes so the image tag must match real commit:
if [[ -n "$(git status --porcelain)" ]]; then
  echo "Uncommitted changes found. Commit before deploying." >&2
  exit 1
fi

SHA=$(git rev-parse --short HEAD)

echo "==> Running tests"
./mvnw -B -q test

echo "==> Building ${DOCKER_IMAGE}:${SHA}"
docker build -t "${DOCKER_IMAGE}:${SHA}" -t "${DOCKER_IMAGE}:latest" .

echo "==> Pushing to Docker Hub"
docker push "${DOCKER_IMAGE}:${SHA}"
docker push "${DOCKER_IMAGE}:latest"

echo "==> Triggering Render deploy of ${SHA}"
curl -fsS -G --data-urlencode "imgURL=docker.io/${DOCKER_IMAGE}:${SHA}" "${RENDER_DEPLOY_HOOK_URL}"
echo
echo "==> Deploy triggered: https://dashboard.render.com"