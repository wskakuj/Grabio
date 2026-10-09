@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

echo ============================================================
echo   GRABIO - repo na GitHubie + pierwsze wydanie z APK
echo ============================================================
echo.

where git >nul 2>nul || (echo [BLAD] Brak "git" w PATH. Zainstaluj Git for Windows. & pause & exit /b 1)
where gh  >nul 2>nul || (
  echo [BLAD] Brak GitHub CLI ^(gh^).
  echo   Zainstaluj:  winget install --id GitHub.cli
  echo   Potem raz:   gh auth login
  pause & exit /b 1
)
where python >nul 2>nul || (echo [BLAD] Brak "python" w PATH. Zainstaluj Pythona 3. & pause & exit /b 1)

gh auth status >nul 2>nul || gh auth login

echo.
echo [1/2] Repo + klucz podpisu + sekrety + push...
python setup.py
if errorlevel 1 (echo [BLAD] setup.py nie powiodl sie. & pause & exit /b 1)

echo.
echo [2/2] Wypuszczam pierwsza wersje (Actions zbuduja APK)...
python release.py -k
if errorlevel 1 (echo [BLAD] release.py nie powiodl sie. & pause & exit /b 1)

echo.
echo ============================================================
echo   GOTOWE!
echo   APK pojawi sie za 1-3 minuty tutaj:
echo     https://github.com/wskakuj/Grabio/releases
echo   Zainstaluj go na telefonie - kolejne wersje beda sie
echo   instalowac same.
echo ============================================================
echo.
pause
