# Seed demo data into the kb MySQL container (idempotent, safe to re-run)
Set-Location $PSScriptRoot

Write-Output 'Seeding demo data into kb-mysql ...'
docker compose exec -T mysql sh -c "mysql -uroot -p2314490042 --default-character-set=utf8mb4 kb < /docker-entrypoint-initdb.d/seed-data.sql"
if ($LASTEXITCODE -ne 0) {
    Write-Error 'Seed failed'
    exit 1
}

docker compose exec -T mysql mysql -uroot -p2314490042 -N -e "SELECT CONCAT('users=', (SELECT COUNT(*) FROM kb.t_user)), CONCAT('questions=', (SELECT COUNT(*) FROM kb.t_question)), CONCAT('practices=', (SELECT COUNT(*) FROM kb.t_practice)), CONCAT('interviews=', (SELECT COUNT(*) FROM kb.t_interview));"

Write-Output 'Done. Demo accounts (password 123456): zhangsan / lisi / wangwu / zhaoliu / chenhao'
