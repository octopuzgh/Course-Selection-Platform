@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================================
:: 智能选课平台 - 本地启动脚本 (Windows)
:: ============================================

:: 项目根目录
set PROJECT_DIR=%~dp0
set PROJECT_DIR=%PROJECT_DIR:~0,-1%

:: 端口配置
set BASIC_PORT=8080
set SELECTION_PORT=8081
set STATISTICS_PORT=8082
set FRONTEND_PORT=8088

:show_menu
echo.
echo ==========================================
echo     智能选课平台 - 本地启动脚本
echo ==========================================
echo 1. 启动 basic-service (8080)
echo 2. 启动 selection-service (8081)
echo 3. 启动 statistics-service (8082)
echo 4. 启动前端 (静态页面)
echo 5. 一键启动所有服务
echo 6. 退出
echo ==========================================
echo.
set /p choice=请选择 [1-6]:

if "%choice%"=="1" goto start_basic
if "%choice%"=="2" goto start_selection
if "%choice%"=="3" goto start_statistics
if "%choice%"=="4" goto start_frontend
if "%choice%"=="5" goto start_all
if "%choice%"=="6" goto end
echo 无效选择，请重试
goto show_menu

:start_basic
echo 启动 basic-service (8080)...
cd /d "%PROJECT_DIR%\basic-service"
call mvnw.cmd spring-boot:run
goto end

:start_selection
echo 启动 selection-service (8081)...
cd /d "%PROJECT_DIR%\selection-service"
call mvnw.cmd spring-boot:run
goto end

:start_statistics
echo 启动 statistics-service (8082)...
cd /d "%PROJECT_DIR%\statistics-service"
call mvnw.cmd spring-boot:run
goto end

:start_frontend
echo 启动前端 (%FRONTEND_PORT%)...
cd /d "%PROJECT_DIR%\fronted"
python -m http.server %FRONTEND_PORT%
goto end

:start_all
echo 一键启动所有服务...
:: 启动后端服务
start "basic-service" cmd /k "cd /d %PROJECT_DIR%\basic-service && call mvnw.cmd spring-boot:run"
start "selection-service" cmd /k "cd /d %PROJECT_DIR%\selection-service && call mvnw.cmd spring-boot:run"
start "statistics-service" cmd /k "cd /d %PROJECT_DIR%\statistics-service && call mvnw.cmd spring-boot:run"
:: 启动前端
start "frontend" cmd /k "cd /d %PROJECT_DIR%\fronted && python -m http.server %FRONTEND_PORT%"
echo 所有服务已启动！
echo 前端地址: http://localhost:%FRONTEND_PORT%
echo 后端地址: http://localhost:%BASIC_PORT%
goto end

:end
endlocal