#!/bin/bash
set -e

echo "=== Building Docker image ==="
docker build -t rest-app .

echo "=== Stopping old container ==="
docker stop rest-app 2>/dev/null || true
docker rm rest-app 2>/dev/null || true

echo "=== Starting new container ==="
docker run -d --name rest-app --restart unless-stopped -p 8081:8080 rest-app

echo "=== Done! ==="
