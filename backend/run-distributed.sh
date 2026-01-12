#!/bin/bash
# Script to run multiple distributed crawler instances locally

echo "Starting WebCrawler Distributed System Test..."
echo "================================================"

# Configuration
MAIN_PORT=8080
INSTANCES=3
BASE_DIR="."

# Kill any existing instances
echo "Cleaning up existing instances..."
lsof -ti:${MAIN_PORT} | xargs kill -9 2>/dev/null || true
for ((i=1; i<INSTANCES; i++)); do
    PORT=$((MAIN_PORT + i * 100))
    lsof -ti:${PORT} | xargs kill -9 2>/dev/null || true
done

echo "Starting main instance on port ${MAIN_PORT}..."
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=${MAIN_PORT}" &
MAIN_PID=$!
echo "Main instance PID: ${MAIN_PID}"

# Wait for main instance to start
sleep 10

# Start additional instances
for ((i=1; i<INSTANCES; i++)); do
    PORT=$((MAIN_PORT + i * 100))
    echo "Starting instance $((i+1)) on port ${PORT}..."
    ./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=${PORT}" &
    echo "Instance $((i+1)) PID: $!"
    sleep 5
done

echo ""
echo "================================================"
echo "All ${INSTANCES} instances started!"
echo "Main API: http://localhost:${MAIN_PORT}"
for ((i=1; i<INSTANCES; i++)); do
    PORT=$((MAIN_PORT + i * 100))
    echo "Instance $((i+1)): http://localhost:${PORT}"
done
echo "================================================"
echo ""
echo "Run tests with: mvn test -Dtest=DistributedCrawlerIntegrationTest"
echo ""
echo "Press Ctrl+C to stop all instances..."

# Keep script running
wait
