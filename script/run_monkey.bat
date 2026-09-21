@echo off
setlocal

echo Running Android UI Exerciser Monkey Test...
echo Make sure an emulator or device is connected.

REM The package name
set PACKAGE=com.hxlxz.tomatoclock
REM Number of events
set EVENTS=5000

REM Delegate to PowerShell script for advanced task pinning logic
powershell.exe -ExecutionPolicy Bypass -File "%~dp0run_monkey.ps1"
exit /b %ERRORLEVEL%

echo Monkey test completed successfully!
endlocal
