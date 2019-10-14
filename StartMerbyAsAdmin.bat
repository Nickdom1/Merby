@echo off
net.exe session 1>NUL 2>&1
if %ErrorLevel% equ 0 (
    echo Starting Merby with Admin privileges. . .
    cd /d %~dp0
    start javaw -jar Merby.jar
) else (
    echo Batch file is NOT running as an Admin. Please run Batch file with Admin privileges.
    pause
)