@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo KPSS Anki Studio - Android APK derleme
where gradle >nul 2>nul
if errorlevel 1 (
  echo.
  echo Gradle bulunamadi. Android Studio kurun, bu android klasorunu Android Studio ile acin,
  echo Gradle Sync tamamlandiktan sonra Build ^> Build Bundle(s) / APK(s) ^> Build APK(s) secin.
  echo APK: app\build\outputs\apk\debug\app-debug.apk
  pause
  exit /b 1
)
gradle assembleDebug
if errorlevel 1 exit /b 1
echo.
echo APK hazir: app\build\outputs\apk\debug\app-debug.apk
pause
