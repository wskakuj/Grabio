@echo off
chcp 65001 >nul
cd /d "%~dp0"
python diagnoza.py
echo.
pause
