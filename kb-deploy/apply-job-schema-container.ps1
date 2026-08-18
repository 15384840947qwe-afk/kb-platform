# Create t_job inside container MySQL (file is mounted via mysql/init dir, read inside container)
Set-Location 'e:\javaeeworkspace\kb-deploy'
Copy-Item 'e:\javaeeworkspace\job-schema.sql' '.\mysql\init\job-schema.sql' -Force
docker compose exec -T mysql sh -c 'mysql -uroot -p2314490042 --default-character-set=utf8mb4 < /docker-entrypoint-initdb.d/job-schema.sql' 2>&1 | Select-String -NotMatch 'Using a password'
docker compose exec -T mysql mysql -uroot -p2314490042 -N -e "SELECT CONCAT('t_job cols=', COUNT(*)) FROM information_schema.columns WHERE table_schema='kb' AND table_name='t_job'" 2>&1 | Select-String -NotMatch 'Using a password'
