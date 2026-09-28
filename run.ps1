param (
    [switch]$Build
)

Set-Location -Path $PSScriptRoot

if ($Build -or -not (Test-Path "engine\bin\com")) {
    Write-Host "Compiling MC-Journal..." -ForegroundColor Cyan
    if (-not (Test-Path "engine\bin")) {
        New-Item -ItemType Directory -Path "engine\bin" | Out-Null
    }
    
    $javaFiles = Get-ChildItem -Path "engine\src" -Filter *.java -Recurse | Select-Object -ExpandProperty FullName
    & javac -cp "engine/lib/*" -d engine/bin $javaFiles

    if ($LASTEXITCODE -ne 0) {
        Write-Host "Compilation failed!" -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

Write-Host "Launching MC-Journal..." -ForegroundColor Green
& java -cp "engine/bin;engine/lib/*" com.mcjournal.client.MCJournalApp
