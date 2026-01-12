@echo off
REM Script to run multiple distributed crawler instances locally on Windows

echo Starting WebCrawler Distributed System Test...
echo ================================================

REM Configuration
set MAIN_PORT=8080
set INSTANCES=3

REM Kill any existing instances
echo Cleaning up existing instances...
for /f "tokens=5" %%a in ('netstat -aon ^| find ":%MAIN_PORT%" ^| find "LISTENING"') do taskkill /F /PID %%a 2>nul || true

echo Starting main instance on port %MAIN_PORT%...
start "WebCrawler Main Instance" cmd /k "mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=%MAIN_PORT%"

REM Wait for main instance to start
timeout /t 10 /nobreak

REM Start additional instances
for /l %%i in (1,1,2) do (
    set /a PORT=%MAIN_PORT% + %%i * 100
    echo Starting instance %%i on port !PORT!...
    start "WebCrawler Instance %%i" cmd /k "mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=!PORT!"
    timeout /t 5 /nobreak
)

echo.
echo ================================================
echo All %INSTANCES% instances started!
echo Main API: http://localhost:%MAIN_PORT%
echo ================================================
echo.
echo Run tests with: mvn test -Dtest=DistributedCrawlerIntegrationTest
echo.
pause
