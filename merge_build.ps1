# Merge Build Script
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "--- Financial Risk Prediction System - Merge Build ---" -ForegroundColor Cyan

$ROOT_DIR = Get-Location
$FRONTEND_DIR = Join-Path $ROOT_DIR "frontend"
$BACKEND_DIR = Join-Path $ROOT_DIR "backend"
$STATIC_DIR = Join-Path $BACKEND_DIR "src\main\resources\static"

# 1. Build Frontend
Write-Host "[1/3] Building Frontend (Vue 3)..." -ForegroundColor Yellow
Set-Location $FRONTEND_DIR
npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: Frontend build failed!" -ForegroundColor Red
    Set-Location $ROOT_DIR
    pause
    exit
}

# 2. Sync Static Resources
Write-Host "[2/3] Syncing static resources to backend..." -ForegroundColor Yellow
if (Test-Path $STATIC_DIR) {
    Remove-Item -Path "$STATIC_DIR\*" -Recurse -Force
} else {
    New-Item -ItemType Directory -Path $STATIC_DIR -Force
}

Copy-Item -Path "$FRONTEND_DIR\dist\*" -Destination $STATIC_DIR -Recurse -Force
Write-Host "Sync Complete!" -ForegroundColor Green

# 3. Compile Backend
Write-Host "[3/3] Compiling Backend (SpringBoot)..." -ForegroundColor Yellow
Set-Location $BACKEND_DIR
mvn clean compile
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: Backend compilation failed!" -ForegroundColor Red
    Set-Location $ROOT_DIR
    pause
    exit
}

Write-Host ""
Write-Host "Build Successful! Run .\start_all.ps1 to start." -ForegroundColor Cyan
Write-Host "URL: http://localhost:8080" -ForegroundColor Cyan

Set-Location $ROOT_DIR
pause
