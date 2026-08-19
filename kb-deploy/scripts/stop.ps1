# KB 停止所有 Docker 服务
# 用法：.\kb-deploy\scripts\stop.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$deployDir = Join-Path $root "kb-deploy"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  KB Platform - Stop All Services" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Stopping all containers..." -ForegroundColor Yellow
Push-Location $deployDir
$oldEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
& cmd /c "docker compose down 2>&1" | Out-Null
$ErrorActionPreference = $oldEap
Pop-Location

Write-Host ""
Write-Host "All services stopped." -ForegroundColor Green
Write-Host ""
Write-Host "Note: Data volumes are preserved." -ForegroundColor Gray
Write-Host "  To remove volumes: docker compose down -v" -ForegroundColor Gray
Write-Host ""
