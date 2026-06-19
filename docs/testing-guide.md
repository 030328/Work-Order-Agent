# 测试与验证指南

本文档用于说明本项目的本地测试、前端构建验证、数据库初始化验证和基础手工联调流程。

## 1. 环境要求

- JDK 21
- Maven 3.9+
- Node.js 18+
- Docker Desktop 或 Docker Engine

PowerShell 查看中文文件时建议显式指定 UTF-8：

```powershell
Get-Content -Encoding UTF8 docs\project-guide.md
```

## 2. 后端单元测试

在项目根目录执行：

```powershell
mvn test
```

当前已覆盖的基础测试：

- `wo-common`
  - `JwtUtilTest`：验证 JWT claims、过期 token、非法 token。
  - `WorkOrderStatusTest`：验证工单状态码唯一性和中文描述。
- `wo-service-workflow`
  - `GuardEvaluatorTest`：验证 SpEL 条件表达式、空条件、异常表达式。
  - `ActionExecutorTest`：验证工作流 action bean 的反射执行和缺失 bean 异常。
  - `SlaServiceImplTest`：验证 SLA deadline 计算、deadline 回写、超时工单升级事件。

预期结果：

```text
BUILD SUCCESS
```

如果只想跑某个模块：

```powershell
mvn -pl wo-common test
mvn -pl wo-service-workflow test
```

## 3. 前端构建验证

进入前端目录：

```powershell
cd wo-frontend
npm run build
```

预期结果：

```text
✓ built
```

构建输出中不应再出现以下大 chunk 警告：

```text
Some chunks are larger than 500 kB after minification
```

当前通过按需注册 Element Plus 组件，最大 JS chunk 已控制在 Vite 默认 500 KB 阈值以下。

## 4. 数据库初始化验证

初始化脚本位置：

```text
docker/mysql/init/01-schema.sql
docker/mysql/init/02-data.sql
```

`02-data.sql` 已显式设置：

```sql
SET NAMES utf8mb4;
```

用于保证中文种子数据按 UTF-8 写入。

默认种子账号：

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| admin | admin123 | ADMIN |
| zhangsan | admin123 | AGENT |
| lisi | admin123 | AGENT |
| wangwu | admin123 | USER |
| zhaoliu | admin123 | MANAGER |

如果使用 Docker 初始化 MySQL：

```powershell
cd docker
docker compose -f docker-compose-infra.yml up -d mysql redis nacos
```

初始化后可进入 MySQL 验证：

```powershell
docker exec -it wo-mysql mysql -uroot -proot123 wo_system
```

在 MySQL 中执行：

```sql
SELECT id, username, real_name, department, role FROM sys_user;
SELECT id, order_no, title, status FROM wo_work_order;
```

预期可以看到中文用户、部门和样例工单。

## 5. 基础手工联调流程

先启动基础设施：

```powershell
cd docker
docker compose -f docker-compose-infra.yml up -d
```

再在项目根目录打包后端：

```powershell
mvn package -DskipTests
```

按顺序启动服务：

```powershell
java -jar wo-gateway\target\wo-gateway-1.0.0-SNAPSHOT.jar
java -jar wo-service-user\target\wo-service-user-1.0.0-SNAPSHOT.jar
java -jar wo-service-workorder\target\wo-service-workorder-1.0.0-SNAPSHOT.jar
java -jar wo-service-ai-agent\target\wo-service-ai-agent-1.0.0-SNAPSHOT.jar
```

启动前端：

```powershell
cd wo-frontend
npm run dev
```

浏览器访问：

```text
http://localhost:3000
```

推荐手工验证链路：

1. 使用 `admin/admin123` 登录。
2. 查看工单列表，确认样例工单显示正常。
3. 创建一条新工单。
4. 进入工单详情，查看 AI 分析结果。
5. 打开 AI 助手，输入“帮我查询待处理工单”。
6. 打开知识库页面，搜索“重置密码”。

## 6. 常见问题

### Maven 无法下载依赖

如果看到远程仓库访问失败，先确认网络和 Maven 镜像配置。依赖下载成功后再执行：

```powershell
mvn test
```

### 前端仍显示 chunk 警告

确认 `wo-frontend/src/main.js` 没有恢复成：

```js
app.use(ElementPlus)
```

当前应为按需注册 Element Plus 组件。

### 中文显示乱码

优先确认文件编码为 UTF-8，并在 PowerShell 里使用：

```powershell
Get-Content -Encoding UTF8 文件路径
```

数据库侧确认建库和连接字符集是 `utf8mb4`。
