@echo off
REM DB2H2 Migration Tool Runner Script for Windows
REM Usage: run.bat [config-file] [options]

setlocal enabledelayedexpansion

REM Default values
set CONFIG_FILE=
set JAR_FILE=target\db2h2-migration-1.0.0.jar
set JAVA_OPTS=-Xmx2g -Xms512m
set DRY_RUN=false

REM Function to show usage
:show_usage
echo Usage: %0 [config-file] [options]
echo.
echo Options:
echo   -h, --help          Show this help message
echo   -j, --jar ^<file^>    Specify JAR file path
echo   -m, --memory ^<size^> Set JVM memory ^(e.g., 2g, 512m^)
echo   --dry-run           Show what would be executed without running
echo.
echo Examples:
echo   %0 config.json
echo   %0 config.json --memory 4g
echo   %0 config.json --jar custom-db2h2.jar
echo.
goto :eof

REM Function to check prerequisites
:check_prerequisites
echo [INFO] Checking prerequisites...

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not installed or not in PATH
    exit /b 1
)

REM Check Java version (simplified check)
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
    goto :version_found
)
:version_found

echo [INFO] Java version: !JAVA_VERSION!

REM Check if JAR file exists
if not exist "%JAR_FILE%" (
    echo [WARN] JAR file not found: %JAR_FILE%
    echo [INFO] Building project...
    
    REM Check if Maven is available
    mvn -version >nul 2>&1
    if errorlevel 1 (
        echo [ERROR] Maven is not installed. Please build the project manually.
        exit /b 1
    )
    
    mvn clean package
    if errorlevel 1 (
        echo [ERROR] Failed to build project
        exit /b 1
    )
)

echo [INFO] Prerequisites check completed
goto :eof

REM Function to validate config file
:validate_config
if defined CONFIG_FILE (
    if not exist "%CONFIG_FILE%" (
        echo [ERROR] Configuration file not found: %CONFIG_FILE%
        exit /b 1
    )
    
    echo [INFO] Configuration file validated: %CONFIG_FILE%
)
goto :eof

REM Function to run migration
:run_migration
echo [INFO] Starting DB2H2 migration...

REM Build command
set CMD=java %JAVA_OPTS% -jar %JAR_FILE%

if defined CONFIG_FILE (
    set CMD=%CMD% --config %CONFIG_FILE%
)

REM Add additional arguments
if defined ADDITIONAL_ARGS (
    set CMD=%CMD% %ADDITIONAL_ARGS%
)

echo [INFO] Executing: %CMD%

if "%DRY_RUN%"=="true" (
    echo [INFO] Dry run mode - command would be: %CMD%
    goto :eof
)

REM Execute migration
%CMD%
if errorlevel 1 (
    echo [ERROR] Migration failed!
    exit /b 1
) else (
    echo [INFO] Migration completed successfully!
)
goto :eof

REM Parse command line arguments
:parse_args
set ADDITIONAL_ARGS=

:loop
if "%~1"=="" goto :end_parse

if "%~1"=="-h" goto :show_usage
if "%~1"=="--help" goto :show_usage

if "%~1"=="-j" (
    set JAR_FILE=%~2
    shift
    shift
    goto :loop
)

if "%~1"=="--jar" (
    set JAR_FILE=%~2
    shift
    shift
    goto :loop
)

if "%~1"=="-m" (
    set JAVA_OPTS=-Xmx%~2 -Xms512m
    shift
    shift
    goto :loop
)

if "%~1"=="--memory" (
    set JAVA_OPTS=-Xmx%~2 -Xms512m
    shift
    shift
    goto :loop
)

if "%~1"=="--dry-run" (
    set DRY_RUN=true
    shift
    goto :loop
)

REM Check if it starts with a dash (unknown option)
echo %~1 | findstr /r "^-" >nul
if not errorlevel 1 (
    echo [ERROR] Unknown option: %~1
    call :show_usage
    exit /b 1
)

REM If no config file set yet, this is the config file
if not defined CONFIG_FILE (
    set CONFIG_FILE=%~1
) else (
    REM Additional arguments to pass to the JAR
    set ADDITIONAL_ARGS=!ADDITIONAL_ARGS! %~1
)

shift
goto :loop

:end_parse
goto :eof

REM Main execution
echo [INFO] DB2H2 Migration Tool
echo [INFO] ====================

call :parse_args %*
call :check_prerequisites
call :validate_config
call :run_migration

endlocal 