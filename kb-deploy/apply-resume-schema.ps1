# 给已有t_resume表加公共字段+提交流转列（幂等：已存在则跳过）
$mysql = 'mysql'
$cred = @('-uroot', '-p2314490042', '--default-character-set=utf8mb4')

$exists = & $mysql @cred -N -e "USE kb; SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='kb' AND table_name='t_resume' AND column_name='submit_status';" 2>&1 | Where-Object { $_ -notmatch 'Using a password' }
if ("$exists".Trim() -eq '1') {
    Write-Output 'submit_status列已存在，跳过ALTER'
} else {
    # cmd重定向传原始UTF-8字节，避免PowerShell再编码
    cmd /c "mysql -uroot -p2314490042 --default-character-set=utf8mb4 kb < e:\javaeeworkspace\kb-deploy\resume-alter.sql"
}

# 存量简历回填公共字段：从content_json里抽（JSON_UNQUOTE+JSON_EXTRACT，MySQL5.7+）
$backfill = @"
USE kb;
UPDATE t_resume SET
  name = NULLIF(JSON_UNQUOTE(JSON_EXTRACT(content_json, '$.basics.name')), ''),
  phone = NULLIF(JSON_UNQUOTE(JSON_EXTRACT(content_json, '$.basics.phone')), ''),
  city = NULLIF(JSON_UNQUOTE(JSON_EXTRACT(content_json, '$.basics.city')), ''),
  ai_score = JSON_EXTRACT(analysis_json, '$.score')
WHERE content_json IS NOT NULL AND name IS NULL;
SELECT COUNT(*) AS filled FROM t_resume WHERE name IS NOT NULL;
"@
& $mysql @cred -e $backfill 2>&1 | Where-Object { $_ -notmatch 'Using a password' }

# 验证列数（应为22列）
$check = "USE kb; SELECT COUNT(*) AS col_count FROM information_schema.columns WHERE table_schema='kb' AND table_name='t_resume'; SHOW INDEX FROM t_resume WHERE Key_name IN ('idx_submit','idx_assigned');"
& $mysql @cred -e $check 2>&1 | Where-Object { $_ -notmatch 'Using a password' }
