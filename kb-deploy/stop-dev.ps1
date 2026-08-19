# Stop vite dev server listening on port 5173
$conns = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue
if (-not $conns) {
    Write-Host "Port 5173 is not listening, nothing to stop"
    exit 0
}
$pids = $conns | Select-Object -ExpandProperty OwningProcess -Unique
foreach ($targetPid in $pids) {
    $proc = Get-Process -Id $targetPid -ErrorAction SilentlyContinue
    if ($proc) {
        Write-Host "Stopping $($proc.ProcessName) (PID $targetPid)"
        Stop-Process -Id $targetPid -Force
    }
}
Write-Host "Done"
