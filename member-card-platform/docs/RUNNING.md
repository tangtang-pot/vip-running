# 运行配置与实现范围

## 数据库配置


创建空数据库 `member_platform`，为应用创建最小必要权限的数据库用户。应用读取 `src/main/resources/schema.sql` 初始化表。

PowerShell 示例（请替换环境变量值）：

```powershell
$env:SPRING_PROFILES_ACTIVE='mysql'
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/member_platform?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
$env:DB_USERNAME='member_app'
$env:DB_PASSWORD='<数据库密码>'
$env:ADMIN_PASSWORD='<至少12字符>'
$env:CLERK_PASSWORD='<至少12字符>'
$env:VIEWER_PASSWORD='<至少12字符>'
$env:JWT_SECRET='<至少32字节的随机密钥>'
mvn spring-boot:run
```

默认本地模式未指定 JWT 密钥时，每次启动生成随机密钥，重启后旧令牌失效。指定固定密钥后，令牌有效期为一小时；退出登录会把令牌标识写入数据库撤销表。


## 当前实现范围

列表由后端加载、前端筛选和分页，适合项目演示；CSV导出在浏览器执行。充值渠道字段用于业务登记，未连接外部支付渠道，退款与自动对账未纳入当前实现。

后端使用Spring JDBC访问数据库。自动集成测试采用内存H2，覆盖权限、令牌撤销、交易幂等、账户状态和并发扣款。MySQL配置由启动环境提供，生产环境的容量、备份和部署需要结合目标环境验证。

本地默认账号与示例数据仅用于演示。其他环境应配置独立账号密码、固定JWT密钥及HTTPS；密码和密钥通过环境变量提供，不写入源码。
