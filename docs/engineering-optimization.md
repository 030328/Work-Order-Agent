# 工程化优化说明

本文档说明本次工程化优化做了什么，以及为什么这样调整。

## 1. 配置外置化

原来多个服务的 `application.yml` 里写死了虚拟机 IP、数据库账号、Redis/Nacos/RocketMQ 地址。例如：

```yaml
server-addr: 192.168.213.100:8848
password: root123
```

这会带来几个问题：

- 项目换一台机器就需要改源码。
- 简历项目发给别人后，对方无法直接使用自己的本地环境。
- 敏感配置容易被误提交。

现在改为 Spring Boot 环境变量占位符：

```yaml
server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
password: ${MYSQL_PASSWORD:root123}
```

含义是：

- 如果环境变量存在，就使用环境变量。
- 如果不存在，就使用冒号后的默认值。

这样 IDEA 本地启动、Docker Compose 启动、服务器部署都可以共用同一套代码。

## 2. 环境变量模板

新增 `.env.example`，用于说明项目需要哪些环境变量。

使用方式：

```powershell
Copy-Item .env.example .env
```

然后按本机环境修改 `.env`。真实 `.env` 已经在 `.gitignore` 中忽略，不建议提交。

## 3. 本地默认值

为了降低本地启动成本，默认值统一改成 192.168.213.100：

| 配置 | 默认值 |
| --- | --- |
| MySQL | `192.168.213.100:3306` |
| Redis | `localhost:6379` |
| Nacos | `localhost:8848` |
| Elasticsearch | `http://localhost:9200` |
| RocketMQ | `localhost:9876` |
| Milvus | `localhost:19530` |
| Frontend API | `/api` |

Docker Compose 场景下，可以通过 compose 的 `environment` 覆盖为容器服务名，例如 `mysql`、`redis`、`nacos`。

## 4. 为什么这是工程化优化

工程化不是只让代码能跑，而是让项目更容易：

- 在不同环境部署。
- 被别人拉下来复现。
- 做持续集成。
- 排查配置问题。
- 避免把个人机器 IP 和密钥写进代码。

面试时可以这样讲：

> 我把项目中的中间件地址、数据库账号、模型 API Key 都从源码配置里抽到了环境变量，通过 `.env.example` 给出模板。这样本地、Docker、服务器可以使用同一套代码，只靠环境变量切换部署目标，降低了项目交付和复现成本。

## 5. 验证方式

后端编译和测试：

```powershell
mvn test
```

前端构建：

```powershell
cd wo-frontend
npm run build
```

查看当前配置是否生效，可以启动服务后观察日志中的数据库、Nacos、Redis 连接地址。

前端 API 地址可以通过 Vite 环境变量覆盖：

```powershell
$env:VITE_API_BASE_URL="/api"
cd wo-frontend
npm run build
```
