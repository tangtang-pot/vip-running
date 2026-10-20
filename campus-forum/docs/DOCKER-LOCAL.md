# 本机Docker运行与复现

## 已完成的本机环境

2026-10-20已安装WSL 2内核并导入专用Ubuntu环境 `CampusDocker`，Docker Engine 29.1.3与Compose 2.40.3可用。Linux磁盘实际位于 `G:\DockerWSL\ext4.vhdx`，WSL交换文件也位于G盘；Windows的WSL系统组件及少量配置仍位于C盘。WSL内存上限3GB、2个虚拟CPU、1GB交换文件。未安装Docker Desktop或VMware。

Docker与Compose来自Ubuntu官方签名仓库；MySQL/Redis/RabbitMQ镜像来自Docker官方AWS镜像仓库；Mailpit来自发布者GHCR；Nacos来自官方公告提供的ACR仓库。原Docker Hub网络不可达。WSL旧内核使用iptables-legacy，启动脚本已处理兼容问题。

## 日常运行

当前复用已安装的Windows MySQL80，不启动Docker MySQL。独立测试库为 `campus_forum_integration`；应用使用仅有该库权限的专用账号。真实凭据由Windows按当前用户加密保存在 `.local`，不上传。

在项目目录双击 `start-docker-local.cmd`：启动专用WSL中的Docker、Redis/RabbitMQ/Mailpit，再启动8083联调版Java服务。要求此前已完成WSL安装、JAR构建以及本机数据库配置。浏览器打开 http://127.0.0.1:8083 。默认开发账号见README。

首次配置本机MySQL可运行 `configure-local-db.ps1` 输入账号，再运行 `initialize-local-db.ps1` 创建独立Schema和应用账号。加密配置仅能由本机当前Windows用户解密；复制到另一台电脑后必须重新配置。脚本中的MySQL客户端默认路径针对本机安装，其他安装位置需调整。

## 端口

| 服务 | 本机端口 |
| --- | --- |
| 论坛联调版 | 8083 |
| Windows MySQL | 本机配置窗口指定，通常3306 |
| Redis | 16379 |
| RabbitMQ AMQP | 15672（这里不是管理控制台端口） |
| Mailpit SMTP / 邮箱界面 | 11025 / 8025 |
| Nacos HTTP / gRPC | 18848 / 19848（仅cloud阶段） |
| 网关 | 8082（仅cloud阶段） |

## 分批Cloud验证

Nacos容器和临时微服务在验证结束后已停止，避免常驻占内存。`compose.local.yml` 的cloud profile提供Nacos；`start-cloud-role.ps1 -Role auth/content/interaction/operations/log/gateway`用于逐个启动真实角色。脚本使用本机 `.local/cloud-build` 中的构建产物，该目录不入源码包；在新机器上须按README用cloud profile构建，并将对应产物放到该目录或调整脚本路径。

新机器准备这些产物时，先停止Java进程以释放JAR文件，再在项目目录执行：

```powershell
mvn -Pcloud -DskipTests package
New-Item -ItemType Directory -Force .local/cloud-build/forum-service/target, .local/cloud-build/gateway/target
Copy-Item forum-service/target/forum-service-1.0.0.jar .local/cloud-build/forum-service/target/
Copy-Item gateway/target/gateway-1.0.0.jar .local/cloud-build/gateway/target/
```

然后执行 `docker-campus.cmd compose -p campus-local --profile cloud --env-file .env -f compose.local.yml up -d nacos`，等待注册中心就绪，再分别在独立PowerShell窗口启动所需角色。按本机内存情况分批测试，不建议直接同时启动六个Java进程。

测试采用认证＋网关＋一个业务角色分批验证；详细已执行结果见VERIFICATION.md。若启动所有服务，需要自行评估可用内存。未验证大模型agent。

## 测试与停止

`check-real-middleware.py` 检查真实基础中间件，`check-cloud-routes.py <role>`检查指定已启动的真实服务路由。两个脚本只针对本机测试端口，并会创建独立标记的测试账号与帖子。

停止Java：关闭运行窗口或Ctrl+C。停止Docker容器可在项目目录执行：

```powershell
.\docker-campus.cmd compose -p campus-local --env-file .env -f compose.local.yml stop
```

不要执行 `down -v`，它会删除测试卷。MySQL80由Windows独立管理，本项目停止命令不会停止它。完整整机重启后可重新运行 `start-docker-local.cmd`；本轮实际测试了应用、Nacos、网关、Redis和RabbitMQ的服务重启恢复，没有再次重启整台电脑。

配置文件、密钥、数据库和日志不在源码包中；本地运行凭据和数据库不随源码分发。
