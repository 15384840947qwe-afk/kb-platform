# MinIO 数据导入脚本
# 用途：把导出的 kb-files bucket 数据导入到本地 MinIO
# 前提：
#   1. MinIO 已启动（localhost:9100，账号 admin/admin12345）
#   2. bucket "kb-files" 已创建（MinIO 控制台 http://localhost:9101）
#   3. 本脚本放在 minio-export 目录同级，或修改 $srcDir 路径

$srcDir = Join-Path $PSScriptRoot "kb-files"
if (-not (Test-Path $srcDir)) {
    Write-Error "找不到数据目录: $srcDir"
    Write-Output "请把 kb-files 文件夹放在本脚本同级目录"
    exit 1
}

$minioEndpoint = "http://localhost:9100"
$accessKey = "admin"
$secretKey = "admin12345"
$bucket = "kb-files"

Write-Output "=== MinIO 数据导入 ==="
Write-Output "源目录: $srcDir"
Write-Output "目标: $minioEndpoint/$bucket"
Write-Output ""

# 检查 mc 客户端
$mc = Get-Command mc -ErrorAction SilentlyContinue
if (-not $mc) {
    Write-Output "未检测到 mc (MinIO Client)，尝试用 docker 执行..."
    $mcCmd = "docker run --rm --network host minio/mc"
    $aliasAdd = "$mcCmd alias set local $minioEndpoint $accessKey $secretKey"
    $copyCmd = "$mcCmd cp --recursive $srcDir/ local/$bucket/"
} else {
    Write-Output "检测到 mc: $($mc.Source)"
    $aliasAdd = "mc alias set local $minioEndpoint $accessKey $secretKey"
    $copyCmd = "mc cp --recursive $srcDir/ local/$bucket/"
}

Write-Output ""
Write-Output "执行以下命令导入："
Write-Output "  $aliasAdd"
Write-Output "  $copyCmd"
Write-Output ""
Write-Output "或者手动执行（推荐）："
Write-Output "  1. 安装 mc: https://min.io/docs/minio/linux/reference/minio-mc.html"
Write-Output "  2. mc alias set local http://localhost:9100 admin admin12345"
Write-Output "  3. mc cp --recursive .\kb-files\ local/kb-files/"
Write-Output ""
Write-Output "导入完成后，重启 kb-service 即可看到文档图片。"
