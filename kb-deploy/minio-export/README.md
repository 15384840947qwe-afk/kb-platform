# MinIO 数据导出（文档图片）

本目录包含项目中所有文档附件和图片的 MinIO 对象存储数据。

## 数据规模

- 文件数：5617
- 总大小：约 736 MB
- 内容：文档图片（images/ 5614 个）+ SQL 文件（12/ 3 个）

## 为什么需要这个

项目中的文档图片存储在 MinIO 对象存储中，不在数据库里。协作者全新部署时 MinIO 是空的，文档中的图片会无法加载。

## 如何分享

由于数据量较大（736 MB），**不要提交到 GitHub**。推荐通过网盘分享：

1. 压缩本目录：`Compress-Archive -Path .\kb-files -DestinationPath kb-minio-data.zip`
2. 上传到网盘（百度网盘/阿里云盘/OneDrive 等）
3. 分享链接给协作者

## 协作者导入步骤

1. 下载并解压 `kb-minio-data.zip`，得到 `kb-files` 文件夹
2. 确保 MinIO 已启动（localhost:9100，账号 admin/admin12345）
3. 在 MinIO 控制台（http://localhost:9101）创建 bucket `kb-files`（如果还没有）
4. 运行导入脚本：
   ```powershell
   cd minio-export
   .\import.ps1
   ```
5. 按脚本提示安装 mc（MinIO 客户端）并执行导入命令
6. 重启 kb-service，文档图片即可正常加载

## 导出方法（给维护者）

如果你需要重新导出（比如添加了新图片）：

```powershell
# 从运行的 MinIO 容器复制数据
docker cp minio:/data/kb-files .\kb-deploy\minio-export\kb-files
```

然后重新压缩分享。
