# KB API 冒烟测试
# 用法：.\kb-deploy\scripts\smoke-test.ps1
$ErrorActionPreference = 'Stop'

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  KB Platform - Smoke Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$base = 'http://localhost:8090'

# 管理员登录
Write-Host "[1/6] Admin login..." -ForegroundColor Yellow
$login = Invoke-RestMethod -Uri "$base/kb/auth/login" -Method POST -ContentType 'application/json; charset=utf-8' -Body '{"username":"admin","password":"admin123"}'
Write-Host "  code: $($login.code), role: $($login.data.role)" -ForegroundColor Green
$h = @{ Authorization = 'Bearer ' + $login.data.token }

# 文档搜索
Write-Host "[2/6] Doc search..." -ForegroundColor Yellow
$docs = Invoke-RestMethod -Uri "$base/kb/doc/search?keyword=test" -Headers $h
Write-Host "  code: $($docs.code)" -ForegroundColor Green

# 面试列表
Write-Host "[3/6] Interview list..." -ForegroundColor Yellow
$iv = Invoke-RestMethod -Uri "$base/kb/interview/list" -Headers $h
Write-Host "  code: $($iv.code)" -ForegroundColor Green

# 刷题看板
Write-Host "[4/6] Drill dashboard..." -ForegroundColor Yellow
$dash = Invoke-RestMethod -Uri "$base/kb/drill/dashboard" -Headers $h
Write-Host "  code: $($dash.code)" -ForegroundColor Green

# 演示账号登录
Write-Host "[5/6] Demo user login (zhangsan)..." -ForegroundColor Yellow
$zlogin = Invoke-RestMethod -Uri "$base/kb/auth/login" -Method POST -ContentType 'application/json; charset=utf-8' -Body '{"username":"zhangsan","password":"123456"}'
Write-Host "  code: $($zlogin.code), nickname: $($zlogin.data.nickname)" -ForegroundColor Green
$zh = @{ Authorization = 'Bearer ' + $zlogin.data.token }

# 智能选题
Write-Host "[6/6] Smart pick..." -ForegroundColor Yellow
$smart = Invoke-RestMethod -Uri "$base/kb/drill/pick?mode=smart&n=5" -Headers $zh
Write-Host "  code: $($smart.code), count: $($smart.data.Count)" -ForegroundColor Green

Write-Host ""
Write-Host "All smoke tests passed!" -ForegroundColor Green
Write-Host ""
