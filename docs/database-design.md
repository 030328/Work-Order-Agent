# 数据库设计文档

数据库名：`wo_system`  
字符集：`utf8mb4`  
引擎：`InnoDB`

---

## 一、用户与权限模块

### 1. sys_user（用户表）

存储系统所有用户信息，包括管理员、客服、普通用户。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| username | VARCHAR(50) | 是 | - | 登录名，唯一 |
| password | VARCHAR(255) | 是 | - | BCrypt 加密密码 |
| real_name | VARCHAR(50) | 否 | - | 真实姓名 |
| email | VARCHAR(100) | 否 | - | 邮箱 |
| phone | VARCHAR(20) | 否 | - | 手机号 |
| avatar | VARCHAR(255) | 否 | - | 头像 URL |
| department | VARCHAR(100) | 否 | - | 所属部门 |
| role | VARCHAR(20) | 是 | USER | 角色：ADMIN/MANAGER/AGENT/USER |
| status | TINYINT | 是 | 1 | 状态：1=启用，0=禁用 |
| last_login_time | DATETIME | 否 | - | 最后登录时间 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 是 | 自动更新 | 更新时间 |
| deleted | TINYINT | 是 | 0 | 逻辑删除：0=正常，1=已删除 |

### 2. sys_role_permission（角色权限表）

RBAC 权限控制，定义每个角色拥有的权限。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | 是 | 主键 |
| role | VARCHAR(20) | 是 | 角色名 |
| permission | VARCHAR(100) | 是 | 权限标识，如 `wo:create`、`wo:assign` |

---

## 二、工单核心模块

### 3. wo_work_order（工单主表）

核心业务表，存储所有工单信息。

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| id | BIGINT | 是 | 雪花算法 | 主键，分布式 ID |
| order_no | VARCHAR(32) | 是 | - | 工单编号，如 `WO-20260617-152030-231` |
| title | VARCHAR(200) | 是 | - | 工单标题 |
| description | TEXT | 是 | - | 工单详细描述 |
| category | VARCHAR(30) | 是 | - | 分类：BUG/FEATURE/QUESTION/MAINTENANCE/INCIDENT |
| priority | VARCHAR(10) | 是 | MEDIUM | 优先级：LOW/MEDIUM/HIGH/URGENT |
| status | VARCHAR(30) | 是 | OPEN | 状态：DRAFT/OPEN/IN_PROGRESS/PENDING_REVIEW/RESOLVED/CLOSED/REJECTED |
| creator_id | BIGINT | 是 | - | 创建人 ID → sys_user.id |
| assignee_id | BIGINT | 否 | - | 处理人 ID → sys_user.id |
| department | VARCHAR(100) | 否 | - | 所属部门 |
| sla_deadline | DATETIME | 否 | - | SLA 截止时间 |
| resolved_at | DATETIME | 否 | - | 解决时间 |
| closed_at | DATETIME | 否 | - | 关闭时间 |
| resolution | TEXT | 否 | - | 解决方案描述 |
| tags | VARCHAR(500) | 否 | - | 标签，逗号分隔 |
| ai_summary | TEXT | 否 | - | AI 自动生成的摘要 |
| ai_sentiment | VARCHAR(20) | 否 | - | AI 情绪分析：POSITIVE/NEUTRAL/NEGATIVE |
| ai_category_suggestion | VARCHAR(30) | 否 | - | AI 建议的分类 |
| ai_suggested_solution | TEXT | 否 | - | AI 建议的解决方案 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 是 | 自动更新 | 更新时间 |
| deleted | TINYINT | 是 | 0 | 逻辑删除 |

**状态流转：**
```
DRAFT → OPEN → IN_PROGRESS → PENDING_REVIEW → RESOLVED → CLOSED
                ↑                ↓
                ← REJECTED ←────┘
```

### 4. wo_flow_record（工单流转记录表）

记录工单的每一次状态变更、分配、评论等操作，用于审计追踪。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | 是 | 主键 |
| work_order_id | BIGINT | 是 | 关联工单 ID |
| action | VARCHAR(30) | 是 | 操作类型：CREATE/ASSIGN/STATUS_CHANGE/COMMENT/ESCALATE/RESOLVE/CLOSE |
| from_status | VARCHAR(30) | 否 | 变更前状态 |
| to_status | VARCHAR(30) | 否 | 变更后状态 |
| operator_id | BIGINT | 是 | 操作人 ID |
| comment | TEXT | 否 | 操作备注 |
| attachment_urls | VARCHAR(1000) | 否 | 附件 URL（JSON 数组） |
| is_system | TINYINT | 否 | 是否系统自动操作：0=人工，1=系统 |
| created_at | DATETIME | 是 | 操作时间 |
| updated_at | DATETIME | 是 | 更新时间 |
| deleted | TINYINT | 是 | 逻辑删除 |

### 5. wo_comment（工单评论表）

存储工单的评论和内部备注。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | 是 | 主键 |
| work_order_id | BIGINT | 是 | 关联工单 ID |
| user_id | BIGINT | 是 | 评论人 ID |
| content | TEXT | 是 | 评论内容 |
| is_internal | TINYINT | 否 | 是否内部备注：0=公开，1=仅内部可见 |
| is_ai_generated | TINYINT | 否 | 是否 AI 生成：0=人工，1=AI |
| created_at | DATETIME | 是 | 评论时间 |
| updated_at | DATETIME | 是 | 更新时间 |
| deleted | TINYINT | 是 | 逻辑删除 |

### 6. wo_attachment（工单附件表）

存储工单关联的文件附件信息。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | 是 | 主键 |
| work_order_id | BIGINT | 是 | 关联工单 ID |
| file_name | VARCHAR(200) | 是 | 文件名 |
| file_url | VARCHAR(500) | 是 | 文件存储 URL |
| file_size | BIGINT | 否 | 文件大小（字节） |
| file_type | VARCHAR(50) | 否 | 文件类型（MIME） |
| uploader_id | BIGINT | 是 | 上传人 ID |
| created_at | DATETIME | 是 | 上传时间 |
| updated_at | DATETIME | 是 | 更新时间 |
| deleted | TINYINT | 是 | 逻辑删除 |

---

## 三、工作流引擎模块

### 7. wf_definition（工作流定义表）

定义工单的流转规则，支持多版本。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | 是 | 主键 |
| name | VARCHAR(100) | 是 | 流程名称，如"标准工单流程" |
| description | VARCHAR(500) | 否 | 流程描述 |
| definition_json | TEXT | 是 | 流程定义（JSON 格式，包含状态列表和初始状态） |
| version | INT | 是 | 版本号 |
| status | TINYINT | 是 | 状态：0=草稿，1=启用 |
| created_at | DATETIME | 是 | 创建时间 |
| updated_at | DATETIME | 是 | 更新时间 |

### 8. wf_transition（状态转换规则表）

定义每个状态下可以转换到哪些状态，以及转换的前置条件。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | 是 | 主键 |
| definition_id | BIGINT | 是 | 关联流程定义 ID |
| from_state | VARCHAR(30) | 是 | 源状态 |
| to_state | VARCHAR(30) | 是 | 目标状态 |
| event | VARCHAR(50) | 是 | 触发事件名 |
| guard_condition | VARCHAR(500) | 否 | SpEL 条件表达式 |
| action_class | VARCHAR(200) | 否 | 转换时执行的动作类 |
| required_role | VARCHAR(50) | 否 | 允许触发的角色 |
| sort_order | INT | 否 | 排序 |

### 9. wf_sla_rule（SLA 规则表）

定义不同优先级工单的响应和解决时限。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | 是 | 主键 |
| name | VARCHAR(100) | 是 | 规则名称 |
| priority | VARCHAR(10) | 是 | 适用优先级：LOW/MEDIUM/HIGH/URGENT |
| response_hours | INT | 是 | 首次响应时限（小时） |
| resolve_hours | INT | 是 | 解决时限（小时） |
| escalation_assignee_id | BIGINT | 否 | 超时升级给谁 |
| is_active | TINYINT | 否 | 是否启用 |
| created_at | DATETIME | 是 | 创建时间 |

---

## 四、知识库模块（RAG）

### 10. kb_document（知识库文档表）

存储知识库文档的元数据，实际向量存储在 Milvus 中。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | 是 | 主键 |
| title | VARCHAR(200) | 是 | 文档标题 |
| content | TEXT | 是 | 文档原文内容 |
| source_type | VARCHAR(30) | 是 | 来源类型：HISTORICAL_WO/DOCUMENTATION/FAQ/TEMPLATE |
| source_id | VARCHAR(50) | 否 | 原始来源 ID（如关联的工单 ID） |
| category | VARCHAR(50) | 否 | 分类 |
| chunk_count | INT | 否 | 向量化后的分片数 |
| status | TINYINT | 否 | 状态：0=草稿，1=已发布，2=已归档 |
| created_by | BIGINT | 否 | 创建人 ID |
| created_at | DATETIME | 是 | 创建时间 |
| updated_at | DATETIME | 是 | 更新时间 |

### 11. kb_vector_mapping（向量映射表）

记录 MySQL 文档与 Milvus 向量的对应关系。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | 是 | 主键 |
| document_id | BIGINT | 是 | 关联文档 ID |
| chunk_index | INT | 是 | 分片序号 |
| chunk_text | TEXT | 是 | 分片文本内容 |
| milvus_id | VARCHAR(50) | 是 | Milvus 中的向量 ID |
| token_count | INT | 否 | Token 数量 |
| created_at | DATETIME | 是 | 创建时间 |

---

## 五、AI 会话模块

### 12. ai_session（AI 会话表）

记录用户与 AI Agent 的对话会话。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | VARCHAR(64) | 是 | 会话 ID（UUID） |
| user_id | BIGINT | 是 | 用户 ID |
| title | VARCHAR(200) | 否 | 会话标题（自动生成） |
| work_order_id | BIGINT | 否 | 关联的工单 ID |
| status | TINYINT | 否 | 状态：1=活跃，0=已结束 |
| total_messages | INT | 否 | 总消息数 |
| total_tokens | BIGINT | 否 | 总 Token 消耗 |
| created_at | DATETIME | 是 | 创建时间 |
| last_active_at | DATETIME | 否 | 最后活跃时间 |

### 13. ai_message（AI 消息表）

存储 AI 对话的每一条消息（冷存储，热数据在 Redis）。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | 是 | 主键 |
| session_id | VARCHAR(64) | 是 | 关联会话 ID |
| role | VARCHAR(20) | 是 | 角色：USER/ASSISTANT/TOOL/SYSTEM |
| content | TEXT | 否 | 消息内容 |
| tool_calls | JSON | 否 | 工具调用详情（AI 触发时） |
| tool_call_id | VARCHAR(100) | 否 | 工具调用 ID（工具返回时） |
| token_count | INT | 否 | 消息 Token 数 |
| created_at | DATETIME | 是 | 创建时间 |

---

## 表关系图

```
sys_user ←──────── wo_work_order ────────→ wo_flow_record
   ↑                    ↑                        ↑
   │                    │                        │
   ├── wo_comment ──────┤                        │
   ├── wo_attachment ───┘                        │
   │                                             │
   ├── ai_session ─────────→ ai_message          │
   │                                             │
   └── kb_document ─────────→ kb_vector_mapping  │
                               (Milvus 向量)      │
                                                  │
wf_definition ────→ wf_transition                │
                                                  │
wf_sla_rule ─────────────────────────────────────┘
```
