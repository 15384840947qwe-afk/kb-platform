# KB 灌入演示数据
# 用法：.\kb-deploy\scripts\seed.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$deployDir = Join-Path $root "kb-deploy"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  KB Platform - Seed Demo Data" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Seeding demo data into kb-mysql..." -ForegroundColor Yellow
Push-Location $deployDir
docker compose exec -T mysql sh -c "mysql -uroot -p2314490042 --default-character-set=utf8mb4 kb < /docker-entrypoint-initdb.d/seed-data.sql"
if ($LASTEXITCODE -ne 0) {
    Write-Host "  Seed failed!" -ForegroundColor Red
    Pop-Location
    exit 1
}

# 显示统计
docker compose exec -T mysql mysql -uroot -p2314490042 -N -e "SELECT CONCAT('users=', (SELECT COUNT(*) FROM kb.t_user)), CONCAT(' questions=', (SELECT COUNT(*) FROM kb.t_question)), CONCAT(' practices=', (SELECT COUNT(*) FROM kb.t_practice)), CONCAT(' interviews=', (SELECT COUNT(*) FROM kb.t_interview));" 2>&1 | Select-String -NotMatch 'Using a password'
Pop-Location

Write-Host ""
Write-Host "Demo accounts (password: 123456):" -ForegroundColor Green
Write-Host "  zhangsan / lisi / wangwu / zhaoliu / chenhao" -ForegroundColor White
Write-Host ""
