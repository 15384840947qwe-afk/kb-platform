# Apply job-schema.sql to local MySQL (cmd redirection passes raw UTF-8 bytes, no re-encoding)
cmd /c 'mysql -uroot -p2314490042 --default-character-set=utf8mb4 < e:\javaeeworkspace\job-schema.sql'
$check = "USE kb; SHOW TABLES LIKE 't_job'; SELECT COUNT(*) AS col_count FROM information_schema.columns WHERE table_schema='kb' AND table_name='t_job';"
& mysql -uroot -p2314490042 --default-character-set=utf8mb4 -e $check 2>&1 | Where-Object { $_ -notmatch 'Using a password' }
