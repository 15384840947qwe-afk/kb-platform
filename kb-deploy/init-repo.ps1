# Init repo, stage files, verify no secrets/artifacts staged, then commit
Set-Location 'e:\kb-platform'

git init 2>&1 | Out-Null
git add .

# Self-check: staged files must NOT contain .env, node_modules, target, dist
$staged = git ls-files --cached
$bad = $staged | Where-Object { $_ -match '(^|/)\.env$|node_modules|/target/|/dist/|\.class$' }
if ($bad) {
    Write-Output 'ABORT: forbidden files staged:'
    $bad | ForEach-Object { Write-Output ('  ' + $_) }
    exit 1
}
Write-Output ('Staged files: ' + $staged.Count + ', forbidden files: 0')

# Verify .gitignore works: .env should be ignored
$ignored = git check-ignore kb-deploy/.env
Write-Output ('kb-deploy/.env ignored by git: ' + [bool]$ignored)

git commit -m "KB knowledge platform: docs collab + AI drill + mock interview + resume assistant, Docker one-click deploy" 2>&1 | Out-String | Write-Output
git log --oneline
