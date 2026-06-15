-- ============================================================
-- Seed Data
-- ============================================================

USE wo_system;

-- ============================================================
-- Admin User (password: admin123, BCrypt encoded)
-- ============================================================
INSERT INTO sys_user (id, username, password, real_name, email, phone, department, role, status) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 'admin@wo-system.com', '13800000000', '技术部', 'ADMIN', 1),
(2, 'zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '张三', 'zhangsan@wo-system.com', '13800000001', '技术部', 'AGENT', 1),
(3, 'lisi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '李四', 'lisi@wo-system.com', '13800000002', '运营部', 'AGENT', 1),
(4, 'wangwu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '王五', 'wangwu@wo-system.com', '13800000003', '产品部', 'USER', 1),
(5, 'zhaoliu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '赵六', 'zhaoliu@wo-system.com', '13800000004', '技术部', 'MANAGER', 1);

-- ============================================================
-- Role Permissions
-- ============================================================
INSERT INTO sys_role_permission (role, permission) VALUES
('ADMIN', 'wo:create'), ('ADMIN', 'wo:read'), ('ADMIN', 'wo:update'), ('ADMIN', 'wo:delete'),
('ADMIN', 'wo:assign'), ('ADMIN', 'wo:admin'), ('ADMIN', 'user:manage'), ('ADMIN', 'kb:manage'),
('MANAGER', 'wo:create'), ('MANAGER', 'wo:read'), ('MANAGER', 'wo:update'), ('MANAGER', 'wo:assign'),
('AGENT', 'wo:create'), ('AGENT', 'wo:read'), ('AGENT', 'wo:update'),
('USER', 'wo:create'), ('USER', 'wo:read');

-- ============================================================
-- Default Workflow Definition
-- ============================================================
INSERT INTO wf_definition (id, name, description, definition_json, version, status) VALUES
(1, '标准工单流程', '标准的工单处理流程，包含从创建到关闭的完整生命周期',
 '{"states":["DRAFT","OPEN","IN_PROGRESS","PENDING_REVIEW","RESOLVED","CLOSED","REJECTED"],"initialState":"DRAFT"}',
 1, 1);

-- Workflow Transitions
INSERT INTO wf_transition (definition_id, from_state, to_state, event, required_role, sort_order) VALUES
(1, 'DRAFT', 'OPEN', 'submit', 'USER', 1),
(1, 'OPEN', 'IN_PROGRESS', 'start', 'AGENT', 2),
(1, 'OPEN', 'REJECTED', 'reject', 'MANAGER', 3),
(1, 'IN_PROGRESS', 'PENDING_REVIEW', 'review', 'AGENT', 4),
(1, 'IN_PROGRESS', 'OPEN', 'reassign', 'MANAGER', 5),
(1, 'PENDING_REVIEW', 'RESOLVED', 'approve', 'USER', 6),
(1, 'PENDING_REVIEW', 'IN_PROGRESS', 'reopen', 'USER', 7),
(1, 'RESOLVED', 'CLOSED', 'close', 'USER', 8),
(1, 'REJECTED', 'OPEN', 'resubmit', 'USER', 9);

-- ============================================================
-- SLA Rules
-- ============================================================
INSERT INTO wf_sla_rule (name, priority, response_hours, resolve_hours, escalation_assignee_id, is_active) VALUES
('紧急工单SLA', 'URGENT', 1, 4, 5, 1),
('高优先级SLA', 'HIGH', 4, 24, 5, 1),
('中优先级SLA', 'MEDIUM', 8, 72, 5, 1),
('低优先级SLA', 'LOW', 24, 168, 5, 1);

-- ============================================================
-- Sample Work Orders
-- ============================================================
INSERT INTO wo_work_order (id, order_no, title, description, category, priority, status, creator_id, assignee_id, department, created_at) VALUES
(1001, 'WO-20260613-0001', '登录页面无法加载', '用户反馈登录页面白屏，控制台报错 net::ERR_CONNECTION_REFUSED', 'BUG', 'HIGH', 'IN_PROGRESS', 4, 2, '技术部', '2026-06-13 10:00:00'),
(1002, 'WO-20260613-0002', '新增数据导出功能', '希望支持将工单数据导出为Excel格式', 'FEATURE', 'MEDIUM', 'OPEN', 4, NULL, '产品部', '2026-06-13 11:00:00'),
(1003, 'WO-20260613-0003', '如何修改个人密码', '忘记密码后如何重置', 'QUESTION', 'LOW', 'RESOLVED', 4, 3, '运营部', '2026-06-13 12:00:00');

-- Sample Comments
INSERT INTO wo_comment (work_order_id, user_id, content, is_internal, is_ai_generated, created_at) VALUES
(1001, 2, '已排查，是Nginx配置问题，正在修复中', 0, 0, '2026-06-13 10:30:00'),
(1001, 1, '已修复Nginx配置，等待用户验证', 1, 0, '2026-06-13 11:00:00'),
(1003, 3, '您可以在个人设置页面点击"修改密码"，如果忘记密码请联系管理员重置', 0, 0, '2026-06-13 12:30:00');

-- Sample Flow Records
INSERT INTO wo_flow_record (work_order_id, action, from_status, to_status, operator_id, comment, created_at) VALUES
(1001, 'CREATE', NULL, 'DRAFT', 4, '创建工单', '2026-06-13 10:00:00'),
(1001, 'STATUS_CHANGE', 'DRAFT', 'OPEN', 4, '提交工单', '2026-06-13 10:01:00'),
(1001, 'ASSIGN', 'OPEN', 'OPEN', 1, '分配给张三', '2026-06-13 10:05:00'),
(1001, 'STATUS_CHANGE', 'OPEN', 'IN_PROGRESS', 2, '开始处理', '2026-06-13 10:10:00'),
(1003, 'CREATE', NULL, 'DRAFT', 4, '创建工单', '2026-06-13 12:00:00'),
(1003, 'STATUS_CHANGE', 'DRAFT', 'OPEN', 4, '提交工单', '2026-06-13 12:01:00'),
(1003, 'STATUS_CHANGE', 'OPEN', 'IN_PROGRESS', 3, '开始处理', '2026-06-13 12:10:00'),
(1003, 'STATUS_CHANGE', 'IN_PROGRESS', 'RESOLVED', 3, '问题已解答', '2026-06-13 12:30:00');

-- ============================================================
-- Sample Knowledge Base Document
-- ============================================================
INSERT INTO kb_document (id, title, content, source_type, source_id, category, status, created_by) VALUES
(1, '常见问题：如何重置密码', '如果忘记密码，请按以下步骤操作：\n1. 点击登录页面的"忘记密码"链接\n2. 输入注册邮箱\n3. 查收重置邮件\n4. 点击邮件中的链接设置新密码\n\n如果无法收到邮件，请联系系统管理员。', 'FAQ', NULL, '账户管理', 1, 1),
(2, '工单处理流程指南', '标准工单处理流程：\n1. 用户创建工单并提交\n2. 系统自动分配或管理员手动分配\n3. 处理人开始处理，更新工单状态\n4. 处理完成后提交验收\n5. 用户验收通过后关闭工单\n\n如需加急处理，请设置优先级为"紧急"。', 'DOCUMENTATION', NULL, '工单管理', 1, 1);
