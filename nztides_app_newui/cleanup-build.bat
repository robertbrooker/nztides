@echo off
echo Stopping Gradle daemons and cleaning build...
taskkill /f /im java.exe 2>nul
timeout /t 2 /nobreak >nul
gradlew --stop
rmdir /s /q app\build 2>nul
rmdir /s /q build 2>nul
echo Cleanup complete. You can now run gradlew assembleDebug
