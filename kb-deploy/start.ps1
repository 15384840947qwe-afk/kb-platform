# KB one-click deploy script
# Usage: .\kb-deploy\start.ps1 [-Rebuild]
# Flow: check docker -> mvn package (skip if jar exists, -Rebuild to force) -> compose up
param([switch]$Rebuild)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

Write-Host "[1/4] Checking docker..." -ForegroundColor Cyan
try { docker version --format '{{.Server.Version}}' | Out-Null } catch {
    Write-Host "Docker is not installed or not running. Please start Docker Desktop first." -ForegroundColor Red
    exit 1
}

Write-Host "[2/4] Building jars (skip if exists, use -Rebuild to force)..." -ForegroundColor Cyan
function Ensure-Jar($proj, $jarName) {
    $jar = Join-Path $root "$proj\target\$jarName"
    if ((Test-Path $jar) -and -not $Rebuild) {
        Write-Host "  $jarName exists, skip"
        return
    }
    Write-Host "  mvn package $proj ..."
    Push-Location (Join-Path $root $proj)
    & .\mvnw.cmd package -DskipTests -q
    if ($LASTEXITCODE -ne 0) { throw "mvn package failed for $proj" }
    Pop-Location
}
Ensure-Jar 'kb-service' 'kb-service-1.0.0.jar'
Ensure-Jar 'kb-gateway' 'kb-gateway-1.0.0.jar'

Write-Host "[3/4] Building images and starting (first run takes several minutes)..." -ForegroundColor Cyan
Push-Location $PSScriptRoot
docker compose up -d --build
if ($LASTEXITCODE -ne 0) { throw "docker compose up failed" }

Write-Host "[4/4] Status (services wait for healthchecks, about 2-3 min):" -ForegroundColor Cyan
docker compose ps

Write-Host ""
Write-Host "Deploy finished! Entries:" -ForegroundColor Green
Write-Host "  Website        http://localhost:8090"
Write-Host "  Nacos console  http://localhost:8849/nacos (nacos/nacos)"
Write-Host "  MinIO console  http://localhost:9201 (admin/admin12345)"
Write-Host "  Admin account  admin / admin123 (seeded on startup)"
Write-Host ""
Write-Host "Useful: docker compose logs -f kb-service | docker compose down (keeps data)"
