@echo off
chcp 65001 >nul
echo ========================================
echo   前端环境切换工具
echo ========================================
echo.

:menu
echo 请选择要切换的环境：
echo.
echo [1] 测试环境 (192.168.137.1)
echo [2] 生产环境 (localhost)
echo [3] 查看当前配置
echo [0] 退出
echo.
set /p choice=请输入选项 (0-3): 

if "%choice%"=="1" goto test_env
if "%choice%"=="2" goto prod_env
if "%choice%"=="3" goto show_config
if "%choice%"=="0" goto end
echo.
echo ❌ 无效选项，请重新选择
echo.
goto menu

:test_env
echo.
echo 正在切换到测试环境...
powershell -Command "(Get-Content index.html) -replace 'window\.BACKEND_HOST = ''[^'']*''', 'window.BACKEND_HOST = ''192.168.137.1''' | Set-Content index.html"
echo ✅ 已切换到测试环境 (192.168.137.1)
echo.
goto show_config

:prod_env
echo.
echo 正在切换到生产环境...
powershell -Command "(Get-Content index.html) -replace 'window\.BACKEND_HOST = ''[^'']*''', 'window.BACKEND_HOST = ''localhost''' | Set-Content index.html"
echo ✅ 已切换到生产环境 (localhost)
echo.
goto show_config

:show_config
echo 当前配置：
findstr /C:"window.BACKEND_HOST" index.html
echo.
echo 💡 提示：修改后需要刷新浏览器页面才能生效
echo.
pause
goto menu

:end
echo.
echo 感谢使用！
exit /b 0
