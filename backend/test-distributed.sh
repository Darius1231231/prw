#!/bin/bash
# Comprehensive test runner for the distributed crawler system

set -e

echo "=========================================="
echo "WebCrawler Distributed System Test Suite"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test functions
test_unit_tests() {
    echo -e "${YELLOW}[1/4] Running Unit Tests...${NC}"
    ./mvnw test -Dtest=NodeServiceTest -q
    echo -e "${GREEN}✓ Unit tests passed${NC}"
}

test_integration_tests() {
    echo -e "${YELLOW}[2/4] Running Integration Tests...${NC}"
    ./mvnw test -Dtest=DistributedCrawlerIntegrationTest -q
    echo -e "${GREEN}✓ Integration tests passed${NC}"
}

test_compile() {
    echo -e "${YELLOW}[3/4] Compiling Project...${NC}"
    ./mvnw clean compile -q
    echo -e "${GREEN}✓ Project compiled successfully${NC}"
}

test_distributed_api() {
    echo -e "${YELLOW}[4/4] Testing Distributed API Endpoints...${NC}"
    
    # Check if server is running
    if ! curl -s http://localhost:8080/api/test/nodes > /dev/null 2>&1; then
        echo -e "${RED}✗ Server not running. Start with: ./mvnw spring-boot:run${NC}"
        return 1
    fi
    
    # Test endpoints
    echo "  Testing GET /api/test/nodes..."
    curl -s http://localhost:8080/api/test/nodes | jq . > /dev/null && echo "    ✓ Passed" || echo "    ✗ Failed"
    
    echo "  Testing GET /api/test/nodes/active..."
    curl -s http://localhost:8080/api/test/nodes/active | jq . > /dev/null && echo "    ✓ Passed" || echo "    ✗ Failed"
    
    echo "  Testing GET /api/test/nodes/stats..."
    curl -s http://localhost:8080/api/test/nodes/stats | jq . > /dev/null && echo "    ✓ Passed" || echo "    ✗ Failed"
    
    echo -e "${GREEN}✓ API tests completed${NC}"
}

# Run tests based on argument
case "${1:-all}" in
    unit)
        test_unit_tests
        ;;
    integration)
        test_integration_tests
        ;;
    compile)
        test_compile
        ;;
    api)
        test_distributed_api
        ;;
    all)
        test_compile
        test_unit_tests
        test_integration_tests
        echo ""
        echo "To test distributed API endpoints:"
        echo "  1. Run: ./mvnw spring-boot:run"
        echo "  2. In another terminal: ./test-distributed.sh api"
        echo ""
        echo -e "${GREEN}All tests completed successfully!${NC}"
        ;;
    *)
        echo "Usage: $0 [all|unit|integration|compile|api]"
        exit 1
        ;;
esac
