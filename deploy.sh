#!/bin/bash
set -e

echo "=== Building Docker image ==="
sudo docker build -t rest-app .

echo "=== Stopping old container ==="
sudo docker stop rest-app 2>/dev/null || true
sudo docker rm rest-app 2>/dev/null || true

echo "=== Starting new container ==="
sudo docker run -d --name rest-app --restart unless-stopped -p 8081:8080 rest-app

echo "=== Done! ==="
