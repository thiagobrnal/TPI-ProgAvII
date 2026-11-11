@echo off
setlocal
cd /d "%~dp0"
title TPI Programacion Avanzada II - Torneos de Futbol Amateur

echo ===============================================================================
echo  TPI Programacion Avanzada II — Sistema de Gestion de Torneos Amateur
echo  Release Candidate 1 (RC1) — UGD (13/11/2026)
echo  Equipo: Thiago Bernal, Tomas Moroz, Lautaro Rodriguez
echo  Docente: Fernando Aguirre
echo ===============================================================================
echo.

set JAVA_CMD=java
if exist "%LOCALAPPDATA%\TPI-PA2\tools\jdk-17\bin\java.exe" (
    set "JAVA_CMD=%LOCALAPPDATA%\TPI-PA2\tools\jdk-17\bin\java.exe"
) else if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    )
)

set JAR_PATH=target\TPI-ProgAv-1.0-SNAPSHOT.jar
if not exist "%JAR_PATH%" (
    if exist "TPI-ProgAv-1.0-SNAPSHOT.jar" (
        set JAR_PATH=TPI-ProgAv-1.0-SNAPSHOT.jar
    ) else (
        echo [ERROR] No se encuentra el archivo JAR empaquetado.
        echo Ejecute 'mvn package -DskipTests' antes de iniciar la aplicacion.
        pause
        exit /b 1
    )
)

if not defined TPI_DB_URL (
    set "TPI_DB_URL=jdbc:postgresql://127.0.0.1:55432/tpi_pa2_r1_demo"
)
if not defined TPI_DB_USER (
    set "TPI_DB_USER=postgres"
)

echo [INFO] Iniciando aplicacion con Java: %JAVA_CMD%
echo [INFO] Base de datos de destino: %TPI_DB_URL%
echo [INFO] Ejecutando: %JAR_PATH%
echo.

"%JAVA_CMD%" -jar "%JAR_PATH%"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] La aplicacion finalizo con codigo de error %ERRORLEVEL%.
    pause
)

endlocal
