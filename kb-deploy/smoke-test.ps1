$base = 'http://localhost:8090'
$login = Invoke-RestMethod -Uri "$base/kb/auth/login" -Method POST -ContentType 'application/json; charset=utf-8' -Body '{"username":"admin","password":"admin123"}'
Write-Host "login code:" $login.code "role:" $login.data.role
$h = @{ Authorization = 'Bearer ' + $login.data.token }
$docs = Invoke-RestMethod -Uri "$base/kb/doc/search?keyword=test" -Headers $h
Write-Host "doc/search code:" $docs.code
$iv = Invoke-RestMethod -Uri "$base/kb/interview/list" -Headers $h
Write-Host "interview/list code:" $iv.code
$dash = Invoke-RestMethod -Uri "$base/kb/drill/dashboard" -Headers $h
Write-Host "dashboard code:" $dash.code

# seeded demo account + AI smart pick
$zlogin = Invoke-RestMethod -Uri "$base/kb/auth/login" -Method POST -ContentType 'application/json; charset=utf-8' -Body '{"username":"zhangsan","password":"123456"}'
Write-Host "zhangsan login code:" $zlogin.code "nickname:" $zlogin.data.nickname
$zh = @{ Authorization = 'Bearer ' + $zlogin.data.token }
$smart = Invoke-RestMethod -Uri "$base/kb/drill/pick?mode=smart&n=5" -Headers $zh
Write-Host "smart pick code:" $smart.code "count:" $smart.data.Count
$wrong = Invoke-RestMethod -Uri "$base/kb/drill/wrong" -Headers $zh
Write-Host "wrong book code:" $wrong.code "count:" $wrong.data.Count
