@echo off
setlocal enabledelayedexpansion
title Jibolo Immunization System Launcher

echo ===================================================
echo   Starting Jibolo Immunization System Launcher...
echo ===================================================

:: Find project root directory containing pom.xml by walking up
set "CURRENT_DIR=%~dp0"
set "ROOT_DIR="

:search_loop
if exist "!CURRENT_DIR!pom.xml" (
    set "ROOT_DIR=!CURRENT_DIR!"
    goto found
)
if exist "!CURRENT_DIR!jibolo\pom.xml" (
    set "ROOT_DIR=!CURRENT_DIR!jibolo\"
    goto found
)
:: Move up one directory level
set "PREV_DIR=!CURRENT_DIR!"
for %%I in ("!CURRENT_DIR!\..") do set "CURRENT_DIR=%%~fI"
if "!CURRENT_DIR!"=="!PREV_DIR!" goto not_found
goto search_loop

:found
echo Looking for project root directory containing pom.xml...
echo Project root found at: !ROOT_DIR!
cd /d "!ROOT_DIR!"

echo.
echo Checking for local XAMPP MySQL database...
set "XAMPP_PATH=C:\xampp"
if exist "!XAMPP_PATH!\mysql\bin\mysqld.exe" (
    echo Found XAMPP installation. Starting MySQL database service in background...
    tasklist /FI "IMAGENAME eq mysqld.exe" 2>NUL | find /I /N "mysqld.exe" >NUL
    if "%ERRORLEVEL%"=="0" (
        echo MySQL database is already running.
    ) else (
        start /B "" "!XAMPP_PATH!\mysql\bin\mysqld.exe" --defaults-file="!XAMPP_PATH!\mysql\bin\my.ini" --standalone
        timeout /t 3 >nul
    )
) else (
    echo XAMPP MySQL not found at %XAMPP_PATH%. Skipping database auto-start...
)

echo.
echo Checking for Maven Wrapper (mvnw.cmd)...
if not exist "mvnw.cmd" (
    echo 'mvnw.cmd' is missing. Automatically setting up Maven tools for you...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw.cmd' -OutFile 'mvnw.cmd'"
    if not exist ".mvn\wrapper" mkdir ".mvn\wrapper"
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/takari/maven-wrapper/master/.mvn/wrapper/maven-wrapper.jar' -OutFile '.mvn\wrapper\maven-wrapper.jar'"
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/takari/maven-wrapper/master/.mvn/wrapper/maven-wrapper.properties' -OutFile '.mvn\wrapper\maven-wrapper.properties'"
)

if exist "mvnw.cmd" (
    echo Starting Spring Boot application via Maven Wrapper...
    echo Application URL will be: http://localhost:8080
    echo ---------------------------------------------------
    start "" http://localhost:8080
    call mvnw.cmd spring-boot:run
) else (
    echo.
    echo ---------------------------------------------------
    echo ERROR: Could not automatically setup Maven tools.
    echo Please make sure you are connected to the internet on your first run.
    echo ---------------------------------------------------
)
goto end

:not_found
echo ERROR: Could not find pom.xml in directory tree.
echo Please make sure this file is placed inside the project folder structure.

:end
echo.
echo The program has stopped running.
pause
