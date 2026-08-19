# Wait until Docker daemon is ready (max ~150s)
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 5
    $out = docker info --format '{{.ServerVersion}}' 2>$null
    if ($LASTEXITCODE -eq 0 -and $out) {
        Write-Output ('Docker ready: ' + $out)
        exit 0
    }
    Write-Output ('waiting... ' + ($i * 5) + 's')
}
Write-Error 'Docker did not become ready in time'
exit 1
