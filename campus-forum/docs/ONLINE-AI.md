# 无 Docker 的在线模型问答

当前单体论坛（8081）支持在线 Chat Completions 接口，默认配置指向 DeepSeek，也可以替换成其他兼容服务的完整接口地址和模型名。参考 [DeepSeek 官方接口文档](https://api-docs.deepseek.com/zh-cn/)。无需 Docker、Ollama、Milvus 或虚拟机。

## 推荐：本地加密配置窗口

运行 `powershell -NoProfile -STA -ExecutionPolicy Bypass -File .\configure-ai-window.ps1`，填写DeepSeek API Key并保存。窗口默认官方接口和deepseek-flash模型。密钥由Windows当前用户加密保存至 `.local/ai-connection.clixml`，此目录同时被Git与Docker构建上下文排除。

使用 `start-docker-local.cmd` 或 `start-middleware-local.ps1` 启动8083版论坛，脚本将加密配置读取到当前Java子进程的环境中；不会把密钥下发浏览器或写入源码。复制源码到另一台电脑后需重新配置，不能直接复制加密文件使用。

## 可选：手动属性文件配置

在项目目录的 PowerShell 中执行 `powershell -ExecutionPolicy Bypass -File .\configure-ai.ps1`，按提示填写接口、模型和 API Key。输入密钥时不会回显。配置只保存在本机的 `local-ai.properties`，已加入忽略清单；文件本身为明文密钥，请勿分享或上传。

也可以复制 `local-ai.properties.example` 为 `local-ai.properties`，填写密钥并将 enabled 改为 true。环境变量 AI_ENABLED、AI_ENDPOINT、AI_MODEL、AI_API_KEY 也可使用，使用时不要再配置同名的本地属性以免覆盖。

关闭原来的论坛 Java 进程，再双击 `start-local.cmd`。用 student 登录，打开资料助手，询问资料中存在的问题，例如“论坛怎么发布帖子”。管理员可以先导入自有资料。配置或密钥错误时会显示明确失败提示，不能据此认为模型已接通。

## 实际行为与限制

- API Key 只由 Java 服务端使用，不下发浏览器。
- 本地先做关键词资料匹配，最多3篇，每篇最多2000字符；问题与这些片段会发送给配置的模型服务商。请仅导入可发送给该服务商的资料。
- 没有匹配资料时直接说明资料不足，不发模型请求。未启用在线模式时保持原来的资料摘录模式。
- 在线模式使用非流式上游调用，完整回答经现有 SSE 接口发送；并非模型逐 token 流式输出。单次请求超时60秒，最多3个并发，不自动重试付费请求。
- 目前为单轮问答与关键词检索，不是 Milvus 向量检索；原独立 agent-service 仍为另一个 Ollama/Milvus 方案。
- 模型调用可能收费。脚本保存配置不会发起请求；只有启用后的有效资料提问才调用模型。

## 验证边界

OnlineAssistantTest 使用本地 HTTP 测试服务校验请求、鉴权、回答解析、错误脱敏与配置检查。这类模拟测试本身不代表服务商真实联调通过。2026-10-20在用户本机已通过论坛完成一次真实DeepSeek调用：验证码有效期问答正确，带1个来源，约1.11秒；另验证未命中资料时本地拒答。具体记录见VERIFICATION.md。
