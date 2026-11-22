@echo off
chcp 65001 >nul
setlocal
set "ROOT=%~dp0"
if not exist "%ROOT%platform-tools\adb.exe" (
  echo ADB not found. Reinstall DevAI Studio with the ADB component.
  pause
  exit /b 1
)
if not exist "%ROOT%DevAI-Android.apk" (
  echo Vexel APK not found. Reinstall DevAI Studio with the Vexel component.
  pause
  exit /b 1
)
"%ROOT%platform-tools\adb.exe" install -r "%ROOT%DevAI-Android.apk"
if errorlevel 1 (
  echo.
  echo Could not install Vexel. Start an Android emulator or connect a phone with USB debugging.
  pause
  exit /b 1
)
"%ROOT%platform-tools\adb.exe" shell am start -n com.devai.studio/com.qwenz.qwel.MainActivity > "%TEMP%\devai-vexel.txt" 2>&1
findstr /I /C:"Error" /C:"Exception" /C:"Permission Denial" "%TEMP%\devai-vexel.txt" >nul
if not errorlevel 1 (
  type "%TEMP%\devai-vexel.txt"
  echo Opening DevAI Studio. Select Vexel in the app.
  "%ROOT%platform-tools\adb.exe" shell monkey -p com.devai.studio -c android.intent.category.LAUNCHER 1
  pause
) else (
  type "%TEMP%\devai-vexel.txt"
)
del "%TEMP%\devai-vexel.txt" >nul 2>&1
