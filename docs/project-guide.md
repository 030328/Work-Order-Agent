# AI Agent 工单系统 — 项目理解指南

> 这份文档帮你快速理解整个项目的架构、核心流程和技术细节，方便面试时讲解。

---

## 一、项目一句话介绍

> 基于 Spring Cloud 微服务架构的企业级工单系统，集成 AI Agent 能力，支持工单智能分析、RAG 知识库检索、工作流引擎和 SLA 监控。

---

## 二、整体架构

```
                        ┌─────────────────┐
                        │   浏览器 / APP    │
                        └────────┬────────┘
                                 │
                        ┌────────▼────────┐
                        │  Spring Cloud    │
                        │    Gateway       │
                        │  (端口 8080)     │
                        │  - 路由转发      │
                        │  - JWT 认证      │
                        │  - 限流          │
                        └────────┬────────┘
                                 │
          ┌──────────────────────┼──────────────────────┐
          │                      │                      │
┌─────────▼─────────┐ ┌─────────▼─────────┐ ┌─────────▼─────────┐
│  wo-service-user   │ │ wo-service-workorder│ │ wo-service-ai-agent│
│  (端口 8081)       │ │  (端口 8082)       │ │  (端口 8084)       │
│  - 注册/登录       │ │  - 工单 CRUD       │ │  - AI 分析         │
│  - JWT 签发        │ │  - 状态流转        │ │  - RAG 知识库      │
│  - 用户管理        │ │  - 评论/附件       │ │  - 通义千问        │
└─────────┬─────────┘ └─────────┬─────────┘ └─────────┬─────────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                        ┌────────▼────────┐
                        │    数据层        │
                        │  MySQL + Redis   │
                        │  Milvus + ES     │
                        └─────────────────┘
```

---

## 三、模块详解

### 3.1 wo-common（公共模块）

**作用：** 所有服务共用的工具类、枚举、异常处理。

**核心类：**
- `R<T>` — 统一响应格式：`{ "code": 0, "message": "success", "data": ... }`
- `BaseEntity` — 基础实体，包含 id、createdAt、updatedAt、deleted
- `BizException` — 业务异常，全局捕获返回友好错误
- `JwtUtil` — JWT 令牌生成和解析
- `RedisService` — Redis 操作封装
- `ErrorCode` — 错误码枚举

**面试话术：**
> "公共模块采用 jar 包方式被其他服务依赖，统一了响应格式、异常处理和工具类，避免代码重复。"

### 3.2 wo-api（接口契约模块）

**作用：** 定义服务间调用的接口和数据结构。

**核心类：**
- `WorkOrderClient` — 调用工单服务的 Feign 接口
- `UserClient` — 调用用户服务的 Feign 接口
- `AiAgentClient` — 调用 AI Agent 服务的 Feign 接口
- 各种 DTO（数据传输对象）— 服务间传递的数据结构

**面试话术：**
> "wo-api 模块是服务间的契约层，通过 OpenFeign 实现声明式 HTTP 调用，配合 Sentinel 实现熔断降级。"

### 3.3 wo-gateway（网关服务）

**作用：** 统一入口，路由转发，JWT 认证。

**核心流程：**
1. 请求到达网关（端口 8080）
2. `RequestLogFilter` 记录请求日志，注入 traceId
3. `JwtAuthFilter` 验证 JWT 令牌
4. 根据路径转发到对应服务：
   - `/api/user/**` → wo-service-user
   - `/api/workorder/**` → wo-service-workorder
   - `/api/ai/**` → wo-service-ai-agent

**面试话术：**
> "网关基于 Spring Cloud Gateway 实现，采用 GlobalFilter 进行 JWT 认证，通过 Nacos 服务发现实现动态路由，集成 Sentinel 做限流保护。"

### 3.4 wo-service-user（用户服务）

**作用：** 用户注册、登录、JWT 签发。

**核心流程：**

```
注册：POST /api/auth/register
  → 校验用户名是否已存在
  → BCrypt 加密密码
  → 保存到 sys_user 表
  → 生成 JWT 令牌
  → 返回 token + 用户信息

登录：POST /api/auth/login
  → 查询用户
  → 校验密码
  → 生成 JWT 令牌
  → 返回 token + 用户信息
```

**关键表：** `sys_user`

**面试话术：**
> "用户服务采用 Spring Security + JWT 实现无状态认证。密码使用 BCrypt 加密存储，JWT 令牌包含用户 ID、用户名和角色信息，通过网关统一校验后传递给下游服务。"

### 3.5 wo-service-workorder（工单服务）

**作用：** 工单的增删改查、状态流转、评论管理。

**核心流程：**

```
创建工单：POST /api/workorders
  → 生成工单编号（WO-日期-时间-随机数）
  → 保存到 wo_work_order 表
  → 保存流转记录到 wo_flow_record 表
  → 调用 AI Agent 服务进行智能分析
  → 保存 AI 分析结果（摘要、情绪、建议）
  → 发送 RocketMQ 事件
  → 返回工单信息
```

**关键表：**
- `wo_work_order` — 工单主表
- `wo_flow_record` — 流转记录
- `wo_comment` — 评论
- `wo_attachment` — 附件

**面试话术：**
> "工单服务是核心业务模块，采用状态机模式管理工单生命周期。创建工单时异步调用 AI Agent 进行智能分析，分析结果自动回填到工单。通过 RocketMQ 实现事件驱动，解耦工单状态变更和后续处理。"

### 3.6 wo-service-ai-agent（AI Agent 服务）

**作用：** AI 智能分析、RAG 知识库检索。

**核心流程：**

```
AI 分析：POST /api/ai/analyze
  → RAG 检索：工单标题+描述 → Embedding → Milvus 搜索相似历史工单
  → 构建 Prompt：注入 RAG 检索结果作为上下文
  → 调用通义千问大模型
  → 解析 JSON 响应
  → 返回分析结果（摘要、情绪、建议分类、建议方案）
```

**技术栈：**
- DashScope SDK — 调用通义千问大模型
- Milvus — 向量数据库，存储知识库文档的向量
- text-embedding-v3 — DashScope 的 Embedding 模型

**面试话术：**
> "AI Agent 服务实现了 RAG（检索增强生成）架构。首先将知识库文档通过 Embedding 模型向量化存入 Milvus，分析工单时先检索相似历史工单，将检索结果注入 Prompt 作为上下文，让大模型参考历史方案给出更准确的建议。"

---

## 四、核心业务流程

### 4.1 用户注册登录流程

```
用户 → Gateway → User 服务
                   ↓
              校验用户名唯一
                   ↓
              BCrypt 加密密码
                   ↓
              保存到 MySQL
                   ↓
              生成 JWT Token
                   ↓
              返回 Token + 用户信息
```

### 4.2 创建工单 + AI 分析流程

```
用户 → Gateway → Workorder 服务
                   ↓
              生成工单编号
                   ↓
              保存工单到 MySQL
                   ↓
              保存流转记录
                   ↓
         ┌─────────┴─────────┐
         ↓                   ↓
    同步返回工单        异步调用 AI Agent
                           ↓
                      RAG 检索知识库
                           ↓
                      构建 Prompt
                           ↓
                      调用通义千问
                           ↓
                      保存 AI 分析结果
```

### 4.3 RAG 知识库流程

```
索引阶段：
  文档内容 → Embedding 模型 → 向量 → 存入 Milvus

检索阶段：
  工单标题+描述 → Embedding → 向量 → Milvus 相似度搜索
                                            ↓
                                     返回相似文档
                                            ↓
                                   注入 AI Prompt 作为参考
```

---

## 五、数据库表关系

```
sys_user (用户表)
    │
    ├──→ wo_work_order (工单表，creator_id / assignee_id)
    │        │
    │        ├──→ wo_flow_record (流转记录)
    │        ├──→ wo_comment (评论)
    │        └──→ wo_attachment (附件)
    │
    ├──→ ai_session (AI 会话)
    │        │
    │        └──→ ai_message (AI 消息)
    │
    └──→ kb_document (知识库文档)
             │
             └──→ kb_vector_mapping (向量映射，对应 Milvus 中的向量)

wf_definition (工作流定义)
    │
    └──→ wf_transition (状态转换规则)

wf_sla_rule (SLA 规则，独立表)
```

---

## 六、API 接口清单

### 用户服务 (8081)
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 注册 |
| POST | /api/auth/login | 登录 |
| GET | /api/users/{id} | 获取用户信息 |
| GET | /api/users | 用户列表 |

### 工单服务 (8082)
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/workorders | 创建工单 |
| GET | /api/workorders | 查询工单列表 |
| GET | /api/workorders/{id} | 工单详情 |
| PUT | /api/workorders/{id}/status | 更新状态 |
| PUT | /api/workorders/{id}/assign | 分配工单 |
| GET | /api/workorders/{id}/comments | 评论列表 |
| POST | /api/workorders/{id}/comments | 添加评论 |

### AI Agent 服务 (8084)
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/ai/analyze | AI 分析工单 |
| POST | /api/ai/knowledge/index | 索引知识库文档 |
| POST | /api/ai/knowledge/search | 搜索知识库 |

---

## 七、技术栈总结

| 技术 | 用途 | 面试怎么说 |
|------|------|-----------|
| Spring Cloud Alibaba | 微服务框架 | "采用 Spring Cloud Alibaba 生态，Nacos 做注册中心和配置中心" |
| Spring Cloud Gateway | API 网关 | "统一入口，JWT 认证，动态路由" |
| OpenFeign | 服务调用 | "声明式 HTTP 调用，配合 Sentinel 熔断降级" |
| Spring Security + JWT | 认证鉴权 | "无状态认证，JWT 令牌包含用户信息" |
| DashScope SDK | 大模型调用 | "调用通义千问进行工单智能分析" |
| RAG + Milvus | 知识库检索 | "检索增强生成，向量相似度搜索" |
| MySQL + MyBatis-Plus | 数据持久化 | "关系型数据库，ORM 采用 MyBatis-Plus" |
| Redis | 缓存 | "会话缓存、分布式锁" |
| RocketMQ | 消息队列 | "事件驱动，解耦工单状态变更" |
| Docker + Docker Compose | 容器化 | "一键部署全套中间件和应用服务" |

---

## 八、面试常见问题

### Q: 为什么用 RAG 而不是直接让 AI 回答？

> "直接让 AI 回答会有幻觉问题，而且不了解我们公司的具体业务。RAG 通过检索历史工单和知识库，让 AI 基于真实数据给出建议，更准确、更可信。"

### Q: 为什么用 Milvus 而不是 Elasticsearch 做向量检索？

> "Milvus 是专业的向量数据库，支持 ANN（近似最近邻）搜索，性能更好。Elasticsearch 虽然也支持向量搜索，但它是通用搜索引擎，向量检索不是它的强项。"

### Q: 工单状态流转是怎么实现的？

> "采用自定义状态机模式，状态转换规则存储在数据库中，支持 SpEL 条件表达式和角色权限控制。相比 Spring StateMachine，更轻量、更灵活，规则可以动态修改不用重启服务。"

### Q: 如何保证工单编号不重复？

> "工单编号格式为 WO-日期-时间-随机数（如 WO-20260617-152030-231），包含日期、时间和三位随机数，基本不会重复。数据库也有唯一约束兜底。"

### Q: AI 分析是同步还是异步？

> "创建工单时同步调用 AI 分析（约 2-3 秒），分析结果直接返回。如果后续需要优化，可以改为异步：先返回工单，AI 分析通过 RocketMQ 异步处理，结果通过 WebSocket 推送。"

### Q: 项目部署架构？

> "开发环境：Windows IDEA 编写代码 + 虚拟机 Docker 运行中间件。生产环境：Docker Compose 编排，一键启动所有服务和中间件。"

---

## 九、项目目录结构

```
Work Order/
├── wo-common/          # 公共模块（工具类、枚举、异常）
├── wo-api/             # 接口契约（Feign 客户端、DTO）
├── wo-gateway/         # 网关服务（路由、认证、限流）
├── wo-service-user/    # 用户服务（注册、登录）
├── wo-service-workorder/  # 工单服务（CRUD、状态流转）
├── wo-service-workflow/   # 工作流引擎（状态机、SLA）
├── wo-service-ai-agent/   # AI Agent（分析、RAG）
├── docker/             # Docker 配置
│   ├── docker-compose.yml
│   └── mysql/init/     # 数据库初始化脚本
└── docs/               # 文档
```

---

## 十、快速上手

### 启动顺序

1. 虚拟机启动中间件：`docker compose -f docker-compose-infra.yml up -d`
2. IDEA 启动 wo-service-user (8081)
3. IDEA 启动 wo-service-workorder (8082)
4. IDEA 启动 wo-service-ai-agent (8084)

### 测试流程

1. 注册用户：`POST /api/auth/register`
2. 登录获取 Token：`POST /api/auth/login`
3. 创建工单：`POST /api/workorders`（自动触发 AI 分析）
4. 查询工单：`GET /api/workorders`
5. 更新状态：`PUT /api/workorders/{id}/status`
6. 索引知识库：`POST /api/ai/knowledge/index`
7. 再次创建工单（AI 会参考知识库）
