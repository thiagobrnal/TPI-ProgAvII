@echo off
setlocal EnableDelayedExpansion
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

REM --------------------------------------------------------------------------
REM  Ejecucion segura (E1 -> E2 / INC-1):
REM  La aplicacion NO aplica ningun valor por defecto de base de datos, activa
REM  el modo aplicacion y rechaza ejecutarse contra la base de pruebas.
REM --------------------------------------------------------------------------
if not defined TPI_DB_URL (
    echo [ERROR] No esta definida la variable de entorno TPI_DB_URL.
    echo.
    echo         La aplicacion exige una URL explicita de base de datos y no
    echo         aplica ningun valor por defecto. No se conectara a ninguna base
    echo         historica o reservada del proyecto.
    echo.
    echo         Defina TPI_DB_URL antes de iniciar, por ejemplo:
    echo             set "TPI_DB_URL=jdbc:postgresql://127.0.0.1:55432/<base_de_ejecucion>"
    echo.
    pause
    exit /b 1
)

if defined TPI_TEST_DB_URL (
    if /I "!TPI_DB_URL!"=="!TPI_TEST_DB_URL!" (
        echo [ERROR] TPI_DB_URL coincide con TPI_TEST_DB_URL.
        echo         No esta permitido ejecutar la aplicacion contra la base de pruebas.
        echo.
        pause
        exit /b 1
    )
)

echo [INFO] Iniciando aplicacion en modo APLICACION con Java: %JAVA_CMD%
echo [INFO] Base de datos de destino configurada por TPI_DB_URL
echo [INFO] Ejecutando: %JAR_PATH%
echo.

"%JAVA_CMD%" -Dtpi.conexion.modo=aplicacion -jar "%JAR_PATH%"
set "EXIT_CODE=%ERRORLEVEL%"
if %EXIT_CODE% NEQ 0 (
    echo.
    echo [ERROR] La aplicacion finalizo con codigo de error %EXIT_CODE%.
    pause
)

endlocal & exit /b %EXIT_CODE%
