# KB 一键部署脚本（Docker 全栈）
# 用法：.\kb-deploy\scripts\start.ps1 [-Rebuild]
param([switch]$Rebuild)
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$deployDir = Join-Path $root "kb-deploy"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  KB Platform - Docker Deploy" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# [1/4] 检查 Docker
Write-Host "[1/4] Checking Docker..." -ForegroundColor Yellow
try {
    $dockerVer = docker version --format '{{.Server.Version}}' 2>$null
    Write-Host "  Docker version: $dockerVer" -ForegroundColor Green
} catch {
    Write-Host "  Docker is not installed or not running!" -ForegroundColor Red
    Write-Host "  Please start Docker Desktop first." -ForegroundColor Red
    exit 1
}

# [2/4] 构建 JAR
Write-Host "[2/4] Building JARs (skip if exists, use -Rebuild to force)..." -ForegroundColor Yellow
function Ensure-Jar($proj, $jarName) {
    $jar = Join-Path $root "$proj\target\$jarName"
    if ((Test-Path $jar) -and -not $Rebuild) {
        Write-Host "  $jarName exists, skip" -ForegroundColor Gray
        return
    }
    Write-Host "  mvn package $proj ..." -ForegroundColor Gray
    Push-Location (Join-Path $root $proj)
    & .\mvnw.cmd package -DskipTests -q
    if ($LASTEXITCODE -ne 0) { throw "mvn package failed for $proj" }
    Pop-Location
}
Ensure-Jar 'kb-service' 'kb-service-1.0.0.jar'
Ensure-Jar 'kb-gateway' 'kb-gateway-1.0.0.jar'

# [3/4] 构建镜像并启动
Write-Host "[3/4] Building images and starting containers..." -ForegroundColor Yellow
Push-Location $deployDir
docker compose up -d --build
if ($LASTEXITCODE -ne 0) {
    Write-Host "  docker compose up failed!" -ForegroundColor Red
    Pop-Location
    exit 1
}
Pop-Location

# [4/4] 显示状态
Write-Host "[4/4] Service status (waiting for healthchecks, ~2-3 min):" -ForegroundColor Yellow
Push-Location $deployDir
docker compose ps
Pop-Location

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Deploy finished!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Website        http://localhost:8090" -ForegroundColor White
Write-Host "  Nacos console  http://localhost:8848/nacos (nacos/nacos)" -ForegroundColor White
Write-Host "  MinIO console  http://localhost:9101 (admin/admin12345)" -ForegroundColor White
Write-Host "  Admin account  admin / admin123" -ForegroundColor White
Write-Host ""
Write-Host "Useful commands:" -ForegroundColor Gray
Write-Host "  docker compose logs -f kb-service" -ForegroundColor Gray
Write-Host "  docker compose down (keeps data)" -ForegroundColor Gray
Write-Host ""
