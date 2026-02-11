#!/bin/bash
# Quick start script for Docling with proper timeout configuration

echo "Starting Docling Serve with 2-hour timeout configuration..."

# Stop and remove existing container
docker stop docling-serve 2>/dev/null && docker rm docling-serve 2>/dev/null
docker stop docling-proxy 2>/dev/null && docker rm docling-proxy 2>/dev/null

# Start Docling with proper configuration
docker run -d \
  --name docling-serve \
  -p 5001:5001 \
  --memory=8g \
  --cpus=4 \
  -e DOCLING_SERVE_API_KEY=default-api-key \
  -e DOCLING_SERVE_ENABLE_UI=true \
  -e DOCLING_SERVE_MAX_SYNC_WAIT=7200 \
  -e DOCLING_SERVE_ENG_LOC_NUM_WORKERS=8 \
  -e OMP_NUM_THREADS=4 \
  -e UVICORN_TIMEOUT_KEEP_ALIVE=7200 \
  ghcr.io/docling-project/docling-serve:v1.9.0 \
  docling-serve run --timeout-keep-alive 7200

echo ""
echo "Waiting for Docling to start..."
sleep 5

# Verify container is running
if docker ps | grep -q docling-serve; then
    echo "✅ Docling started successfully!"
    echo ""
    echo "Container info:"
    docker ps | grep docling
    echo ""
    echo "Environment variables:"
    docker exec docling-serve env | grep -E "DOCLING_SERVE_MAX_SYNC_WAIT|NUM_WORKERS|OMP_NUM_THREADS|UVICORN_TIMEOUT"
    echo ""
    echo "🚀 Docling UI: http://localhost:5001/ui"
    echo "📄 API Docs: http://localhost:5001/docs"
else
    echo "❌ Failed to start Docling!"
    echo "Logs:"
    docker logs docling-serve
    exit 1
fi
