@echo off
echo =====================================================
echo    PDF Processing Platform - Docker
echo =====================================================

docker --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker not installed! Get it from https://docker.com
    pause
    exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker not running! Start Docker Desktop.
    pause
    exit /b 1
)

echo Building and starting containers...
docker-compose up --build -d

if errorlevel 1 (
    echo ERROR: Failed! Run 'docker-compose logs' for details.
    pause
    exit /b 1
)

echo.
echo =====================================================
echo    SUCCESS! Services starting...
echo =====================================================
echo.
echo    API:         http://localhost:8080/api
echo    Swagger:     http://localhost:8080/api/swagger-ui.html
echo    Health:      http://localhost:8080/api/actuator/health
echo.
echo    Logs:        docker-compose logs -f
echo    Stop:        docker-compose down
echo.
pause
