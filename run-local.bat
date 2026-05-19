@echo off
setlocal enabledelayedexpansion

set PROJECT_DIR=%~dp0
set PROJECT_DIR=%PROJECT_DIR:~0,-1%

set BASIC_PORT=8080
set SELECTION_PORT=8081
set STATISTICS_PORT=8082
set FRONTEND_PORT=8088

:show_menu
echo.
echo ==========================================
echo   Course Selection Platform
echo ==========================================
echo [S] Start all services
echo [1] basic-service (8080)
echo [2] selection-service (8081)
echo [3] statistics-service (8082)
echo [4] frontend (8088)
echo -----------------------------------------
echo [K] Kill all services
echo [Q] Quit
echo ==========================================
echo.
set /p choice="Select: "

if /i "%choice%"=="S" goto start_all
if /i "%choice%"=="1" goto start_basic
if /i "%choice%"=="2" goto start_selection
if /i "%choice%"=="3" goto start_statistics
if /i "%choice%"=="4" goto start_frontend
if /i "%choice%"=="K" goto kill_all
if /i "%choice%"=="Q" goto end
goto show_menu

:start_basic
start "basic-service" cmd /k "cd /d %PROJECT_DIR%\basic-service && call mvnw.cmd spring-boot:run"
echo basic-service started
goto show_menu

:start_selection
start "selection-service" cmd /k "cd /d %PROJECT_DIR%\selection-service && call mvnw.cmd spring-boot:run"
echo selection-service started
goto show_menu

:start_statistics
start "statistics-service" cmd /k "cd /d %PROJECT_DIR%\statistics-service && call mvnw.cmd spring-boot:run"
echo statistics-service started
goto show_menu

:start_frontend
start "frontend" cmd /k "cd /d %PROJECT_DIR%\fronted && python -m http.server %FRONTEND_PORT%"
echo frontend started
goto show_menu

:start_all
echo Starting all services...
start "basic-service" cmd /k "cd /d %PROJECT_DIR%\basic-service && call mvnw.cmd spring-boot:run"
start "selection-service" cmd /k "cd /d %PROJECT_DIR%\selection-service && call mvnw.cmd spring-boot:run"
start "statistics-service" cmd /k "cd /d %PROJECT_DIR%\statistics-service && call mvnw.cmd spring-boot:run"
start "frontend" cmd /k "cd /d %PROJECT_DIR%\fronted && python -m http.server %FRONTEND_PORT%"
echo All services started
echo Frontend: http://localhost:%FRONTEND_PORT%
goto show_menu

:kill_all
echo Killing all services...
taskkill /FI "WINDOWTITLE eq basic-service*" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq selection-service*" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq statistics-service*" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq frontend*" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq maven*" /F >nul 2>&1
echo All services killed
goto show_menu

:end
endlocal