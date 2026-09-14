@echo off
set "MAVEN_VERSION=3.9.6"
set "MAVEN_DIR=%~dp0.maven\apache-maven-%MAVEN_VERSION%"
set "MVN_CMD=%MAVEN_DIR%\bin\mvn.cmd"

if not exist "%MAVEN_DIR%" (
    echo Downloading Maven %MAVEN_VERSION%...
    mkdir "%~dp0.maven" 2>nul
    powershell -Command "Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip' -OutFile '%~dp0.maven\maven.zip'"
    echo Extracting Maven...
    powershell -Command "Expand-Archive -Path '%~dp0.maven\maven.zip' -DestinationPath '%~dp0.maven' -Force"
    del "%~dp0.maven\maven.zip"
)

echo Compiling Vending Machine...
cd /d "%~dp0"
call "%MVN_CMD%" clean compile
if %ERRORLEVEL% NEQ 0 (
    echo Compilation Failed!
    pause
    exit /b %ERRORLEVEL%
)
echo Compilation Successful!
pause
