@echo off
REM ==================================================
REM Grabio - jednorazowa konfiguracja GitHuba (2x klik)
REM Repo + klucz podpisu + sekrety + pierwszy push.
REM Wymaga zalogowanego GitHub CLI: gh auth login
REM ==================================================
cd /d "%~dp0"
python setup.py
echo.
pause
