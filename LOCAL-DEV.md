# 本地开发指南（不走一键 Docker，在自己机器上改代码调试）

适用场景：协作者要频繁修改代码、断点调试、体验"保存即生效"。
一键 Docker 部署请看 [README.md](README.md) 的快速开始。

## 依赖清单

kb-service 启动硬依赖 5 个中间件（application.yml 里写死 localhost，连不上就起不来）：

| 中间件 | 端口 | 账号 | 用途 |
|---|---|---|---|
| MySQL 8 | 3306 | root / 2314490042，库名 kb | 业务数据 |
| Redis | 6379 | 无密码 | 限流/缓存 |
| RabbitMQ | 5672 | guest / guest | 审核/文件事件 |
| MinIO | 9100 | admin / admin12345 | 附件对象存储 |
| Nacos | 8848 | 单机 standalone 模式 | 注册/配置中心 |

开发工具：JDK 17、Node.js 18+、Maven 无需安装（项目带 mvnw 包装器）。

## 中间件怎么来：两种方式

### 方式 A：Docker 只跑中间件（推荐）

不用在 Windows 上装任何东西，一条命令起齐，端口已还原成 application.yml 期望的本地端口：

```powershell
cd kb-deploy
docker compose -f docker-compose.yml -f compose-dev.yml up -d mysql redis rabbitmq minio nacos
```

- 首次启动 MySQL 容器会自动执行 `mysql/init/*.sql` 建库建表灌种子，零手工导入
- 数据存在 Docker volume 里，`docker compose down` 不丢数据
- **注意**：如果机器上已经装过本地 MySQL/Redis/Nacos，先停掉，避免端口冲突
- 验证：`docker compose ps` 全部 healthy 后，浏览器开 http://localhost:8848/nacos （nacos/nacos）

停止：`docker compose down`（带 `-v` 会连数据卷一起删，慎用）。

### 方式 B：全部本机安装

| 中间件 | Windows 获取方式 |
|---|---|
| MySQL 8 | 官网 MSI Installer，安装时 root 密码设成 2314490042，或装完改 application.yml |
| Redis | https://github.com/tporadowski/redis/releases （Windows 版） |
| RabbitMQ | 先装 Erlang 再装 RabbitMQ（默认 guest/guest 即可） |
| MinIO | 官网下载 minio.exe，`minio.exe server D:\minio-data --console-address ":9101"` |
| Nacos | GitHub 下载 zip 解压，`bin\startup.cmd -m standalone` |

装完需要手动建库导表：

```powershell
# 依次执行三个 SQL（先建库建表，后灌种子数据）
mysql -uroot -p2314490042 < kb-deploy\mysql\init\job-schema.sql
mysql -uroot -p2314490042 --default-character-set=utf8mb4 < kb-deploy\mysql\init\kb-schema.sql
mysql -uroot -p2314490042 --default-character-set=utf8mb4 < kb-deploy\mysql\init\seed-data.sql
```

另需在 MinIO 控制台（http://localhost:9101）手动建 bucket `kb-files`。

## 启动业务代码

```powershell
# 1. 后端核心服务（8082，context-path /kb）
cd kb-service
.\mvnw.cmd spring-boot:run

# 2. 网关（9001，前端 dev 代理指到它）——另开一个窗口
cd kb-gateway
.\mvnw.cmd spring-boot:run

# 3. 前端（5173，保存代码浏览器自动热更新）——再开一个窗口
cd kb-web
npm install
npm run dev
```

想启用 AI 功能（刷题批改/面试点评/简历分析/AI出题）：启动后端前设置环境变量

```powershell
$env:AI_API_KEY = "sk-你的硅基流动密钥"   # 仅当前窗口有效；永久生效用 setx AI_API_KEY "sk-..."
```

不设置也能跑，AI 功能自动降级（简答自评/本地规则）。

浏览器打开 http://localhost:5173 ，管理员 `admin / admin123`。

## 改代码后如何生效

| 改了什么 | 怎么测 |
|---|---|
| kb-web 前端（.vue/.js） | **保存即热更新**，浏览器自动刷新，唯一不用重启的 |
| kb-service / kb-gateway 后端 | 停掉重跑 `.\mvnw.cmd spring-boot:run`（IDEA 里点重启更快） |
| mysql/init 下的建表 SQL | 方式 A：`docker compose down -v` 后重新 up（会重建库）；方式 B：手动重新执行 SQL |

## 常见问题

- **kb-service 启动报 9848/8848 连不上**：Nacos 没起，先起 Nacos 再起后端
- **启动报连接 MySQL 拒绝**：中间件没起或密码不是 2314490042，对照上面清单
- **方式 A 端口被占**：`netstat -ano | findstr :3306` 找出占用进程，停掉本机装的服务
- **上传附件报 bucket 不存在**：MinIO 控制台建 `kb-files` bucket
- **本机有 VMware/WSL 虚拟网卡导致网关转发挂起**：application.yml 已钉死 127.0.0.1 注册 IP，无需处理
