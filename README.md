# KB 知识库 · 求职工作台

> 文档协作 + AI 刷题 + 模拟面试 + 简历助手，一站式学习与求职备考平台。
> 不绑定技术岗——教师、运营、产品等知识型岗位同样适用。

## ✨ 功能亮点

### 📚 知识库（团队协作）
- 多级目录 + 富文本文档，支持审核流（提交 → 管理员审核 → 发布）
- 附件上传（MinIO 对象存储）、阅读历史、文档内 AI 问答
- **RAG 向量检索**：文档自动切块 + bge-m3 向量化，问答走混合检索（余弦相似 + 关键词），
  embedding 不可用时自动退纯关键词，管理员可一键重建全库索引

### 🎯 AI 刷题
- 从文档一键 AI 出题（单选/多选/填空/简答四种题型）
- **按岗位 AI 出题**：解析岗位 JD 后一键出面试简答题，自动入题库（同题干去重）；
  出题接 RAG 接地（知识库资料 + 题库相似题），贴合站内知识不撞题
- 简答题 AI 流式批改，逐字上屏
- **AI 针对练**：按分科目正确率加权抽题，优先练薄弱点
- 错题本：答对一次即毕业

### 🎤 模拟面试
- 对话式面试 + 语音朗读，AI 逐题流式点评
- 面试报告：分维度评分 + 改进建议，成长看板追踪分数曲线

### 📄 简历助手
- PDF/TXT/MD 导入，AI 自动结构化提取
- AI 四维分析（完整度/表述质量/岗位匹配度/排版规范）
- **JD 匹配**：粘贴目标岗位 JD，输出匹配分 + 已覆盖项 + 缺失项 + 改进建议
- AI 一键生成/润色，实时预览，浏览器打印导出成品 PDF
- **投递与审阅闭环**：用户提交简历 → 管理员审阅（通过/驳回，终态互斥）→ 按简历推荐匹配岗位，
  用户端可查看推荐岗位并按岗位生成专属面试题

### 💼 岗位中心
- 爬虫采集真实招聘数据（kb-spider，独立仓库），管理员审核后上架
- **AI 岗位解析**：从 JD 提炼技能/年限/学历/关键词等结构化需求，列表标记「已AI解析」
- 管理员可按岗位一键 AI 出题入库；岗位数据驱动简历匹配推荐

### 🛡️ 工程能力
- AI 接口按用户维度限流（Redis + Lua，真 HTTP 429）
- **AI 双模型自动降级**：主模型超时/报错自动切备用模型，流式调用「一字未吐」才重试，
  云端模型宕机功能不断摆；启动日志 AI 配置自检（key 脱敏）
- Docker Compose 一键部署（8 容器全链路，含健康检查依赖编排）
- SSE 流式传输全链路（后端 → 网关 → Nginx 关缓冲）

## 🖼️ 界面预览

<!-- 在这里放截图，建议 4 张：主页 / 刷题 / 面试 / 简历 -->
<!-- 放到 docs/screenshots/ 目录后改成：<img src="docs/screenshots/home.png" width="420"> -->

## 🏗️ 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + Element Plus + Vite |
| 后端 | Spring Boot 3 + Spring Cloud Gateway + Nacos |
| 持久层 | MyBatis-Plus + MySQL 8 |
| 中间件 | Redis（限流/缓存）、RabbitMQ（审核/文件事件）、MinIO（对象存储） |
| AI | OpenAI 兼容协议（SiliconFlow / DeepSeek / Ollama 均可） |
| 部署 | Docker Compose + Nginx（SSE 流式代理） |

## 🚀 快速开始（Docker 一键部署）

**环境要求**：Docker Desktop + PowerShell

```powershell
# 1. 配置 AI 密钥（刷题批改/面试点评/简历分析需要）
cd kb-deploy
copy .env.example .env       # 然后编辑 .env，填入自己的 AI_API_KEY

# 2. 一键构建并启动（首次约 5-10 分钟）
.\start.ps1

# 3. 灌入演示数据（可选）
.\seed.ps1
```

启动完成后访问：

| 入口 | 地址 |
|---|---|
| 网站 | http://localhost:8090 |
| Nacos 控制台 | http://localhost:8849/nacos （nacos/nacos） |
| MinIO 控制台 | http://localhost:9201 （admin/admin12345） |

**账号**：
- 管理员：`admin / admin123`
- 演示账号（seed 后）：`zhangsan / lisi / wangwu / zhaoliu / chenhao`，密码均 `123456`

## 💻 本地开发（可选）

```powershell
# 后端（需本机 MySQL/Redis/RabbitMQ/MinIO/Nacos，见 kb-service/application.yml）
cd kb-service
.\mvnw.cmd spring-boot:run

# 前端
cd kb-web
npm install
npm run dev        # http://localhost:5173
```

本地跑想启用 AI 功能：设置环境变量 `AI_API_KEY=你的key`（IDEA 运行配置的 Environment variables 里加即可）。

## 📁 项目结构

```
kb-platform/
├── kb-service/     # 核心业务服务（8082）：文档/刷题/面试/简历/限流
├── kb-gateway/     # Spring Cloud Gateway（9001）：路由 + JWT 鉴权
├── kb-web/         # Vue3 前端（多阶段 Docker 构建，Nginx 托管）
└── kb-deploy/      # 部署目录
    ├── docker-compose.yml      # 8 容器编排
    ├── start.ps1               # 一键部署（-Rebuild 强制重打包）
    ├── seed.ps1                # 演示数据灌入（幂等）
    ├── mysql/init/             # 建表 + 种子数据（首次启动自动执行）
    ├── .env.example            # 环境变量模板（.env 不入库）
    └── gen/                    # BCrypt 哈希生成小工具
```

## ⚙️ 配置说明

| 变量 | 说明 | 位置 |
|---|---|---|
| `AI_API_KEY` | AI 大模型密钥（OpenAI 兼容协议，如 SiliconFlow） | kb-deploy/.env |
| `AI_FALLBACK_MODEL` | 备用模型（主模型故障自动切换，默认 DeepSeek-V3，可选） | 环境变量 |
| `JWT_SECRET` | JWT 签名密钥（可选，有默认值） | 环境变量 |
| `MINIO_PUBLIC_ENDPOINT` | 附件下载外链地址（可选） | 环境变量 |

演示默认密码（数据库 root 等）见 docker-compose.yml，仅供本地演示，生产请自行更换。

## 📌 说明

- AI 服务为可选插件：不配置 `AI_API_KEY` 时系统自动降级（简答自评/本地规则检查），其余功能不受影响
- 存量文档想启用 RAG 向量检索：管理员在首页点「重建索引」，或执行 `kb-deploy/rag-alter.sql` 后 POST `/kb/doc/reindex`
- 数据持久化在 Docker volume 中，`docker compose down` 不丢数据；`down -v` 删卷后首次启动会自动重建库并灌入种子数据
