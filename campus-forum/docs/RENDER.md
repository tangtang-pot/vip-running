# Render 免费演示部署

本配置仅运行 forum-service，使用临时文件型 H2。无需 MySQL、Redis、RabbitMQ、Nacos 或模型 API 密钥。

## 部署

1. 在 Render 控制台选择 New → Blueprint，连接包含 campus-forum 的 GitHub 仓库。
2. Blueprint Path 填写 `campus-forum/render.yaml`。
3. 核对服务为 Free。ADMIN_PASSWORD 和 DEMO_PASSWORD 分别设置至少 12 字符的管理员、student 账号密码；不要使用本地默认密码。
4. 确认创建后等待构建与健康检查通过，打开平台分配的 HTTPS 网址。

如果使用 New → Web Service 手动部署：Branch 为 main，Root Directory 为 campus-forum，Language 为 Docker，Dockerfile Path 为 ./Dockerfile.render，实例为 Free。按 render.yaml 设置环境变量，并设置至少 32 字节的 JWT_SECRET；健康检查路径填写 /api/categories。

Blueprint 已设置 `rootDir: campus-forum`，Docker 路径以该项目目录为基础，不需要重复添加 campus-forum 前缀。

## 演示范围

- 可用：演示账号登录、帖子、评论、点赞收藏、管理后台。
- 默认关闭：真实模型调用、外部中间件、开发验证码回显。未配置 SMTP 的此演示不提供可完成的邮箱注册，请使用初始化账号。
- 不上传 .env、local-ai.properties、本机数据库或模型密钥。
- Free 服务在 15 分钟无请求后会休眠，重新访问需要等待启动。
- H2 数据会在服务休眠、重启或重新部署后重置，不适合保存真实业务数据。

## 验证

部署日志应显示 Java 应用启动完成。访问 /api/categories 应返回分类 JSON；使用 admin 和 student 配置的密码登录。密钥和密码仅在 Render 环境变量中配置。
