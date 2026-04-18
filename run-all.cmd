@echo off
REM run-all.cmd - wrapper to run run-all.ps1 without changing execution policy
REM Usage: run-all.cmd -RestartDelay 2

set SCRIPT_DIR=%~dp0
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%run-all.ps1" %*
