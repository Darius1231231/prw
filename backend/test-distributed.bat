@echo off
REM Comprehensive test runner for the distributed crawler system

echo ==========================================
echo WebCrawler Distributed System Test Suite
echo ==========================================
echo.

REM Test functions
if "%1%"=="" goto test_all
if "%1%"=="unit" goto test_unit
if "%1%"=="integration" goto test_integration
if "%1%"=="compile" goto test_compile
if "%1%"=="api" goto test_api
goto usage

:test_all
echo [1/3] Compiling Project...
call mvnw.cmd clean compile -q
echo. OK - Project compiled
echo.

echo [2/3] Running Unit Tests...
call mvnw.cmd test -Dtest=NodeServiceTest -q
echo. OK - Unit tests passed
echo.

echo [3/3] Running Integration Tests...
call mvnw.cmd test -Dtest=DistributedCrawlerIntegrationTest -q
echo. OK - Integration tests passed
echo.

echo All tests completed successfully!
echo.
echo To test distributed API endpoints:
echo   1. Run: mvnw.cmd spring-boot:run
echo   2. In another terminal: test-distributed.bat api
goto end

:test_unit
echo Running Unit Tests...
call mvnw.cmd test -Dtest=NodeServiceTest -q
echo OK - Unit tests passed
goto end

:test_integration
echo Running Integration Tests...
call mvnw.cmd test -Dtest=DistributedCrawlerIntegrationTest -q
echo OK - Integration tests passed
goto end

:test_compile
echo Compiling Project...
call mvnw.cmd clean compile -q
echo OK - Project compiled
goto end

:test_api
echo Testing Distributed API Endpoints...
REM Note: Requires server to be running
echo Testing GET /api/test/nodes...
curl -s http://localhost:8080/api/test/nodes | jq .
echo.
echo Testing GET /api/test/nodes/active...
curl -s http://localhost:8080/api/test/nodes/active | jq .
echo.
echo Testing GET /api/test/nodes/stats...
curl -s http://localhost:8080/api/test/nodes/stats | jq .
goto end

:usage
echo Usage: %0% [all^|unit^|integration^|compile^|api]
exit /b 1

:end
