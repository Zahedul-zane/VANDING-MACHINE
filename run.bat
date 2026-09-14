@echo off
set "MAVEN_VERSION=3.9.6"
set "MAVEN_DIR=%~dp0.maven\apache-maven-%MAVEN_VERSION%"
set "MVN_CMD=%MAVEN_DIR%\bin\mvn.cmd"

echo Starting Vending Machine Application...
cd /d "%~dp0"
call "%MVN_CMD%" javafx:run
pause
