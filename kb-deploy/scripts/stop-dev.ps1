# KB 本地开发一键停止脚本
# 用法：.\kb-deploy\scripts\stop-dev.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$deployDir = Join-Path $root "kb-deploy"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  KB Platform - Local Dev Stopper" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ── 停止 Java 进程（gateway + service） ─────────────────
Write-Host "[1/3] Stopping Java services..." -ForegroundColor Yellow
$javaProcs = Get-CimInstance Win32_Process | Where-Object {
    $_.CommandLine -match 'kb-gateway|kb-service' -and $_.Name -eq 'java.exe'
}
if ($javaProcs) {
    $javaProcs | ForEach-Object {
        Write-Host "  Killing PID $($_.ProcessId)..." -ForegroundColor Gray
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }
} else {
    Write-Host "  No Java services found." -ForegroundColor Gray
}

# ─ 停止 Node 进程（Vite） ─────────────────────────────
Write-Host "[2/3] Stopping Vite dev server..." -ForegroundColor Yellow
# 杀掉监听 5173 端口的 node 进程
$nodePort = Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique
if ($nodePort) {
    $nodePort | ForEach-Object {
        Write-Host "  Killing Node PID $_..." -ForegroundColor Gray
        Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue
    }
} else {
    Write-Host "  No Vite server found." -ForegroundColor Gray
}

# ── 停止中间件容器 ──────────────────────────────────────
Write-Host "[3/3] Stopping middleware containers..." -ForegroundColor Yellow
Push-Location $deployDir
# docker compose 把进度信息写 stderr，PowerShell 会当异常抛出，用 cmd /c 绕过
$oldEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
& cmd /c "docker compose stop mysql redis rabbitmq minio nacos 2>&1" | Out-Null
$ErrorActionPreference = $oldEap
Pop-Location

Write-Host ""
Write-Host "All services stopped." -ForegroundColor Green
Write-Host ""
