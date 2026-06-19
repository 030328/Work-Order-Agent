USE wo_system;
SET NAMES utf8mb4;

ALTER TABLE wf_sla_rule CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE wo_comment CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

UPDATE wf_sla_rule
SET name = '紧急工单SLA'
WHERE priority = 'URGENT';

UPDATE wf_sla_rule
SET name = '高优先级SLA'
WHERE priority = 'HIGH';

UPDATE wf_sla_rule
SET name = '中优先级SLA'
WHERE priority = 'MEDIUM';

UPDATE wf_sla_rule
SET name = '低优先级SLA'
WHERE priority = 'LOW';

UPDATE wo_comment
SET content = '已排查，是Nginx配置问题，正在修复中'
WHERE work_order_id = 1001
  AND user_id = 2
  AND created_at = '2026-06-13 10:30:00';

UPDATE wo_comment
SET content = '已修复Nginx配置，等待用户验证'
WHERE work_order_id = 1001
  AND user_id = 1
  AND created_at = '2026-06-13 11:00:00';

UPDATE wo_comment
SET content = '您可以在个人设置页面点击"修改密码"，如果忘记密码请联系管理员重置'
WHERE work_order_id = 1003
  AND user_id = 3
  AND created_at = '2026-06-13 12:30:00';
