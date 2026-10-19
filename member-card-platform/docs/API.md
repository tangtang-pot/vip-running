# HTTP API

所有接口以 `/api` 开头。除登录外需要 `Authorization: Bearer <token>`。金额字段为整数分。错误响应包含 `message`；身份认证失败返回 401，无权限返回 403，参数错误返回 400，数据库唯一约束冲突返回 409。

| 方法 | 路径 | 说明 / 权限 |
| --- | --- | --- |
| POST | /auth/login | username、password，返回 token、expiresIn |
| GET | /auth/me | 当前用户和权限 |
| POST | /auth/logout | 撤销当前令牌 |
| GET | /members | 已登录用户查询会员 |
| GET | /members/{id} | 查询会员详情 |
| POST | /members | 新增；members 权限 |
| PUT | /members/{id} | 编辑；members 权限 |
| PATCH | /members/{id}/status | 正常 / 停用；members 权限 |
| POST | /members/{id}/transactions | 充值需 recharge；消费需 spend |
| GET | /transactions | transactions 权限 |
| GET | /logs | audit 权限 |
| GET | /roles | 已登录用户查看角色定义 |
| PUT | /roles/{id} | 修改非管理员角色；roles 权限 |

创建或编辑会员：

```json
{"name":"演示会员","phone":"13800001234","level":"普通会员","note":"会员备注"}
```

充值 100 元：

```json
{"type":"充值","amount":10000,"channel":"微信","note":"演示充值","requestKey":"a-unique-uuid-for-this-payment"}
```

消费相同结构，`type` 为 `消费`，后端将渠道固定为 `会员余额`。消费每满 1 元积 1 分，充值不赠积分。

修改角色：

```json
{"permissions":["members","recharge","spend","transactions"]}
```

当前 API 返回完整列表，筛选和分页由前端执行。浏览器导出 CSV 不会发送新增导出审计事件；生产使用应补充有服务端授权和审计的导出接口。
