#!/bin/bash

# Docker Test Script for Wild Weather Update Plugin
# Tests the plugin in a containerized environment

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Ensure plugin is built
if [[ ! -f "target/wild-weather-update-1.0.0-SNAPSHOT.jar" ]]; then
    print_error "Plugin JAR not found. Run 'make build' first."
    exit 1
fi

print_status "Starting Docker test for Wild Weather Update plugin..."

# Stop any existing containers
print_status "Stopping existing containers..."
docker-compose down 2>/dev/null || true

# Start the container
print_status "Starting Minecraft server container..."
docker-compose up -d

# Wait for server to start
print_status "Waiting for server to start..."
sleep 30

# Check if container is running
if ! docker-compose ps | grep -q "Up"; then
    print_error "Container failed to start"
    docker-compose logs
    exit 1
fi

print_success "Container is running"

# Wait for Minecraft server to be ready
print_status "Waiting for Minecraft server to be ready..."
timeout=120
counter=0

while [[ $counter -lt $timeout ]]; do
    if docker-compose logs | grep -q "Done"; then
        print_success "Minecraft server is ready!"
        break
    fi
    sleep 2
    ((counter += 2))
    echo -n "."
done

if [[ $counter -ge $timeout ]]; then
    print_error "Server startup timeout"
    docker-compose logs
    exit 1
fi

echo ""

# Check if plugin loaded
print_status "Checking if Wild Weather Update plugin loaded..."
if docker-compose logs | grep -q "Wild Weather Update has been enabled"; then
    print_success "Wild Weather Update plugin loaded successfully!"
else
    print_warning "Plugin load status unclear, checking logs..."
    docker-compose logs | grep -i "wild weather" || true
fi

# Test plugin commands
print_status "Testing plugin commands..."
docker-compose exec minecraftbe bash -c "echo 'weather help' > /minecraft/stdin.txt"
sleep 2

# Show recent logs
print_status "Recent server logs:"
docker-compose logs --tail=20

# Keep container running for manual testing
print_status "Container is running for manual testing"
print_status "Connect to: localhost:25565"
print_status "Bedrock connect to: localhost:19132"
print_status ""
print_status "Test commands:"
print_status "  /weather help"
print_status "  /weather trigger monsoon"
print_status "  /weather status"
print_status "  /weather debug monsoon"
print_status ""
print_status "To stop the test environment:"
print_status "  docker-compose down"
print_status ""
print_status "To view logs:"
print_status "  docker-compose logs -f"

print_success "Docker test setup complete!"
