@echo off
setlocal EnableDelayedExpansion

set BUILD_TYPE=%1
if "%BUILD_TYPE%"=="" set BUILD_TYPE=Release

set SCRIPT_DIR=%~dp0
set BUILD_DIR=%SCRIPT_DIR%build
set OUTPUT_DIR=%SCRIPT_DIR%..\src\main\resources\native\windows-x64
set CMAKE_PATH=C:\Program Files\CMake\bin\cmake.exe

echo ============================================
echo AITT Native Library Build
echo ============================================
echo.

if "%JAVA_HOME%"=="" (
    echo ERROR: JAVA_HOME is not set
    exit /b 1
)

echo JAVA_HOME: %JAVA_HOME%
echo Build Type: %BUILD_TYPE%
echo.

if not exist "%CMAKE_PATH%" (
    echo ERROR: CMake not found at %CMAKE_PATH%
    exit /b 1
)

if /I "%BUILD_TYPE%"=="clean" (
    echo Cleaning build directory...
    if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
    echo Done.
    exit /b 0
)

if /I "%BUILD_TYPE%"=="rebuild" (
    if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
    set BUILD_TYPE=Release
)

echo Configuring CMake...
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"

"%CMAKE_PATH%" -B "%BUILD_DIR%" -S "%SCRIPT_DIR%" -G "Visual Studio 17 2022" -A x64
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: CMake configuration failed
    exit /b 1
)

echo.
echo Building %BUILD_TYPE%...
"%CMAKE_PATH%" --build "%BUILD_DIR%" --config %BUILD_TYPE% --parallel
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build failed
    exit /b 1
)

echo.
echo ============================================
echo Build completed successfully!
echo ============================================

set DLL_PATH=%BUILD_DIR%\%BUILD_TYPE%\aitt_native.dll
if exist "%DLL_PATH%" (
    echo Output: %DLL_PATH%
    if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"
    copy /Y "%DLL_PATH%" "%OUTPUT_DIR%\"
    echo Copied to: %OUTPUT_DIR%
)

endlocal
