# vip-running

此仓库按子目录组织项目。

## 项目

- [会员卡运营平台](member-card-platform/README.md)：基于 Spring Boot 的会员、储值交易与权限管理平台。
- [校园论坛](campus-forum/README.md)：校园交流、权限管理、Redis/RabbitMQ、Nacos 网关与可选 DeepSeek 资料问答。

## 运行会员卡平台

需要 JDK 17 或更高版本和 Maven 3.6.3 或更高版本。

```bash
cd member-card-platform
mvn test
mvn spring-boot:run
```

详细配置、功能说明和页面截图见[项目说明](member-card-platform/README.md)。

## 运行校园论坛

需要 JDK 21 或更高版本和 Maven 3.6.3 或更高版本。默认 H2 模式无需 Docker 或模型密钥。

```bash
cd campus-forum
mvn test
mvn -pl forum-service spring-boot:run
```

打开 http://127.0.0.1:8081 。真实中间件运行、在线模型配置和本机验证记录见[校园论坛说明](campus-forum/README.md)。API Key、数据库凭据、加密配置和本机运行数据不在仓库内；源码上传不代表已完成公网生产部署。
