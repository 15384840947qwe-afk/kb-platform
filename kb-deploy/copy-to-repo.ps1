# Copy the 4 projects into e:\kb-platform, excluding build artifacts/deps/IDE files
$dst = 'e:\kb-platform'
if (-not (Test-Path $dst)) { New-Item -ItemType Directory -Path $dst | Out-Null }

$src = 'e:\javaeeworkspace\kb-modular'
robocopy "$src\kb-service" "$dst\kb-service" /E /XD target .idea .git /NFL /NDL /NJH /NJS /NC /NS
robocopy "$src\kb-gateway" "$dst\kb-gateway" /E /XD target .idea .git /NFL /NDL /NJH /NJS /NC /NS
robocopy "$src\kb-web"     "$dst\kb-web"     /E /XD node_modules dist .idea .git /NFL /NDL /NJH /NJS /NC /NS
robocopy "$src\kb-deploy"  "$dst\kb-deploy"  /E /XD .git minio-export\kb-files /NFL /NDL /NJH /NJS /NC /NS
# robocopy exit codes 0-7 all mean success
if ($LASTEXITCODE -ge 8) { Write-Error 'robocopy failed'; exit 1 }

Write-Output 'Copy done. Top level of kb-platform:'
Get-ChildItem $dst | Select-Object -ExpandProperty Name
Write-Output ('kb-deploy\.env exists: ' + (Test-Path "$dst\kb-deploy\.env"))
Write-Output ('kb-web\node_modules excluded: ' + (-not (Test-Path "$dst\kb-web\node_modules")))
