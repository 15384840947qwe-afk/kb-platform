# KB 本地开发一键启动脚本
# 用法：.\kb-deploy\scripts\start-dev.ps1 [-SkipMiddleware]
# 功能：Docker 起中间件 + 本地跑 gateway/service/web（每个服务一个窗口）
param([switch]$SkipMiddleware)

$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$deployDir = Join-Path $root "kb-deploy"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  KB Platform - Local Dev Launcher" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ── Step 1: 启动中间件容器 ──────────────────────────────
if (-not $SkipMiddleware) {
    Write-Host "[1/4] Starting middleware containers..." -ForegroundColor Yellow
    Push-Location $deployDir
    $composeFile = Join-Path $deployDir "docker-compose.yml"

    # 只启动中间件 5 个服务，不启动 kb-service/gateway/web
    docker compose -f $composeFile up -d mysql redis rabbitmq minio nacos
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  Failed to start containers!" -ForegroundColor Red
        Pop-Location
        exit 1
    }

    Write-Host "  Waiting for health checks (this may take 30-60s)..." -ForegroundColor Gray

    # 等待中间件就绪
    $services = @("kb-mysql", "kb-redis", "kb-rabbitmq", "kb-minio", "kb-nacos")
    $timeout = 120
    $start = Get-Date
    $allReady = $false

    while ((New-TimeSpan -Start $start -End (Get-Date)).TotalSeconds -lt $timeout) {
        $allReady = $true
        foreach ($svc in $services) {
            $status = docker inspect --format '{{.State.Health.Status}}' $svc 2>$null
            if ($status -ne "healthy") {
                $allReady = $false
                break
            }
        }
        if ($allReady) { break }
        Start-Sleep -Seconds 3
    }

    if ($allReady) {
        Write-Host "  All middleware ready!" -ForegroundColor Green
    } else {
        Write-Host "  Warning: some containers may not be healthy yet, continuing anyway..." -ForegroundColor Yellow
    }
    Pop-Location
} else {
    Write-Host "[1/4] Skipping middleware startup (-SkipMiddleware)" -ForegroundColor Gray
}

# ── 加载 .env 到本地环境变量（供 kb-service 等本地进程读取） ──
$envFile = Join-Path $deployDir ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#') -and $line -match '^([A-Z_][A-Z0-9_]*)=(.*)$') {
            [System.Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
        }
    }
    Write-Host "  Loaded env from .env" -ForegroundColor Gray
}

# ── Step 2: 启动 kb-gateway ─────────────────────────────
Write-Host "[2/4] Starting kb-gateway (port 9001)..." -ForegroundColor Yellow
$gwDir = Join-Path $root "kb-gateway"
Start-Process powershell -ArgumentList "-NoExit", "-Command",
    "Set-Location '$gwDir'; Write-Host '=== kb-gateway ===' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run" `
    -WindowStyle Normal
Start-Sleep -Seconds 3

# ── Step 3: 启动 kb-service ─────────────────────────────
Write-Host "[3/4] Starting kb-service (port 8082)..." -ForegroundColor Yellow
$svcDir = Join-Path $root "kb-service"
# 拼出环境变量设置命令，让子进程能读到 .env 里的 AI_API_KEY 等
$envSetup = ''
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#') -and $line -match '^([A-Z_][A-Z0-9_]*)=(.*)$') {
            $envSetup += "`$env:$($Matches[1])='$($Matches[2])'; "
        }
    }
}
Start-Process powershell -ArgumentList "-NoExit", "-Command",
    "$envSetup Set-Location '$svcDir'; Write-Host '=== kb-service ===' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run" `
    -WindowStyle Normal
Start-Sleep -Seconds 3

# ── Step 4: 启动 kb-web ─────────────────────────────────
Write-Host "[4/4] Starting kb-web (Vite dev server)..." -ForegroundColor Yellow
$webDir = Join-Path $root "kb-web"
Start-Process powershell -ArgumentList "-NoExit", "-Command",
    "Set-Location '$webDir'; Write-Host '=== kb-web ===' -ForegroundColor Cyan; npm run dev" `
    -WindowStyle Normal

# ── 完成 ─────────────────────────────────────────────────
Start-Sleep -Seconds 2
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  All services launched!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Website        http://localhost:5173  (Vite dev)" -ForegroundColor White
Write-Host "  Gateway        http://localhost:9001" -ForegroundColor White
Write-Host "  Service        http://localhost:8082" -ForegroundColor White
Write-Host "  Nacos console  http://localhost:8848/nacos (nacos/nacos)" -ForegroundColor White
Write-Host "  MinIO console  http://localhost:9101 (admin/admin12345)" -ForegroundColor White
Write-Host ""
Write-Host "  Each service runs in its own window." -ForegroundColor Gray
Write-Host "  Close a window to stop that service." -ForegroundColor Gray
Write-Host "  Run .\scripts\stop-dev.ps1 to stop everything." -ForegroundColor Gray
Write-Host ""
