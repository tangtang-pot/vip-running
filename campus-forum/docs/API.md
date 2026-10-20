# 校园论坛 API

除公开浏览、分类、热榜、注册与登录外，接口需要 `Authorization: Bearer <JWT>`。所有写入接口使用 JSON；金额等会员平台功能不属于此项目。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /api/auth/code | 邮箱申请验证码；开发模式返回 devCode |
| POST | /api/auth/register | username、email、password、code |
| POST | /api/auth/login | username、password，返回 token |
| GET | /api/auth/me | 当前账号及角色 |
| POST | /api/auth/logout | 撤销当前令牌 |
| GET | /api/categories | 帖子分类 |
| GET | /api/posts | q、category、page、size、bookmarks 筛选；size最多50 |
| GET | /api/posts/{id} | 帖子、评论与当前用户互动 |
| POST | /api/posts | title、body、category |
| PUT | /api/posts/{id} | 作者编辑 |
| DELETE | /api/posts/{id} | 作者软删除 |
| POST | /api/posts/{id}/comments | body 评论 |
| PUT | /api/posts/{id}/reactions | kind=like或bookmark，active=true或false |
| GET | /api/hot | 最多5条热门帖子 |
| GET | /api/admin/summary | 管理员统计 |
| GET | /api/admin/posts | 管理员帖子列表 |
| PATCH | /api/admin/posts/{id} | status=published或hidden |
| GET | /api/admin/logs | 最近200条审计记录，仅管理员 |
| POST | /api/knowledge | title、content，管理员资料导入 |
| GET | /api/assistant/stream?q=... | SSE问答，问题最多500字符 |

SSE 事件：`mode` 表示运行模式；`source` 为来源；`token` 为正文；`done` 表示完成。前端通过 Fetch ReadableStream 携带 Authorization 读取 SSE，不将令牌拼接到网址。

错误码：400参数或验证码错误、401未登录或令牌失效、403权限不足、404帖子不可见、409唯一约束冲突、429访问过于频繁。
