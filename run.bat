@echo off
cd /d "%~dp0"

if "%1"=="--build" goto build
if "%1"=="-b" goto build
if not exist "engine\bin\com" goto build
goto run

:build
echo Compiling MC-Journal...
if not exist "engine\bin" mkdir "engine\bin"
javac -cp "engine/lib/*" -d engine/bin engine/src/com/mcjournal/block/property/*.java engine/src/com/mcjournal/block/*.java engine/src/com/mcjournal/physics/*.java engine/src/com/mcjournal/*.java engine/src/com/mcjournal/test/*.java engine/src/com/mcjournal/client/*.java engine/src/com/mcjournal/client/gui/*.java
if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

:run
echo Launching MC-Journal...
java -cp "engine/bin;engine/lib/*" com.mcjournal.client.MCJournalApp
