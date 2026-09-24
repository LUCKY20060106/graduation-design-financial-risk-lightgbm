# Start All Script
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "--- Financial Risk Prediction System - Start Script ---" -ForegroundColor Cyan

$ROOT_DIR = Get-Location

# 1. Start Docker DB
Write-Host "[1/3] Starting Docker DB (bishe-mysql)..." -ForegroundColor Yellow
docker start bishe-mysql
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: Docker container failed to start. Ensure Docker Desktop is running!" -ForegroundColor Red
    pause
    exit
}

# 2. Wait for DB
Write-Host "Waiting for database to be ready (5s)..." -ForegroundColor Gray
Start-Sleep -s 5

# 3. Clean Port 8080
Write-Host "[2/3] Checking and cleaning port 8080..." -ForegroundColor Yellow
$portProcess = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($portProcess) {
    Write-Host "Found process occupying port 8080, PID: $($portProcess.OwningProcess)" -ForegroundColor Magenta
    Stop-Process -Id $portProcess.OwningProcess -Force -ErrorAction SilentlyContinue
}

# 4. Start Backend
Write-Host "[3/3] Starting SpringBoot backend..." -ForegroundColor Green
$BACKEND_DIR = Join-Path $ROOT_DIR "backend"
Set-Location $BACKEND_DIR
mvn spring-boot:run
