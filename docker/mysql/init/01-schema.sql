-- ============================================================
-- AI Agent Work Order System - Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS wo_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE wo_system;

-- ============================================================
-- User and RBAC Tables
-- ============================================================

CREATE TABLE sys_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(50) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    real_name       VARCHAR(50),
    email           VARCHAR(100),
    phone           VARCHAR(20),
    avatar          VARCHAR(255),
    department      VARCHAR(100),
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',
    status          TINYINT NOT NULL DEFAULT 1,
    last_login_time DATETIME,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    INDEX idx_username (username),
    INDEX idx_department (department)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role_permission (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    role            VARCHAR(20) NOT NULL,
    permission      VARCHAR(100) NOT NULL,
    UNIQUE KEY uk_role_perm (role, permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Work Order Core Tables
-- ============================================================

CREATE TABLE wo_work_order (
    id                      BIGINT PRIMARY KEY,
    order_no                VARCHAR(32) NOT NULL UNIQUE,
    title                   VARCHAR(200) NOT NULL,
    description             TEXT NOT NULL,
    category                VARCHAR(30) NOT NULL,
    priority                VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    status                  VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    creator_id              BIGINT NOT NULL,
    assignee_id             BIGINT,
    department              VARCHAR(100),
    sla_deadline            DATETIME,
    resolved_at             DATETIME,
    closed_at               DATETIME,
    resolution              TEXT,
    tags                    VARCHAR(500),
    ai_summary              TEXT,
    ai_sentiment            VARCHAR(20),
    ai_category_suggestion  VARCHAR(30),
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                 TINYINT NOT NULL DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_assignee (assignee_id),
    INDEX idx_creator (creator_id),
    INDEX idx_priority_status (priority, status),
    INDEX idx_created_at (created_at),
    INDEX idx_category (category),
    FULLTEXT INDEX ft_title_desc (title, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wo_flow_record (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    work_order_id   BIGINT NOT NULL,
    action          VARCHAR(30) NOT NULL,
    from_status     VARCHAR(30),
    to_status       VARCHAR(30),
    operator_id     BIGINT NOT NULL,
    comment         TEXT,
    attachment_urls VARCHAR(1000),
    is_system       TINYINT DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    INDEX idx_wo_id (work_order_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wo_comment (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    work_order_id   BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    content         TEXT NOT NULL,
    is_internal     TINYINT DEFAULT 0,
    is_ai_generated TINYINT DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    INDEX idx_wo_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wo_attachment (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    work_order_id   BIGINT NOT NULL,
    file_name       VARCHAR(200) NOT NULL,
    file_url        VARCHAR(500) NOT NULL,
    file_size       BIGINT,
    file_type       VARCHAR(50),
    uploader_id     BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    INDEX idx_wo_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Workflow Engine Tables
-- ============================================================

CREATE TABLE wf_definition (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    definition_json TEXT NOT NULL,
    version         INT NOT NULL DEFAULT 1,
    status          TINYINT NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wf_transition (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    definition_id   BIGINT NOT NULL,
    from_state      VARCHAR(30) NOT NULL,
    to_state        VARCHAR(30) NOT NULL,
    event           VARCHAR(50) NOT NULL,
    guard_condition VARCHAR(500),
    action_class    VARCHAR(200),
    required_role   VARCHAR(50),
    sort_order      INT DEFAULT 0,
    INDEX idx_def_state (definition_id, from_state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wf_sla_rule (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    name                    VARCHAR(100) NOT NULL,
    priority                VARCHAR(10) NOT NULL,
    response_hours          INT NOT NULL,
    resolve_hours           INT NOT NULL,
    escalation_assignee_id  BIGINT,
    is_active               TINYINT DEFAULT 1,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Knowledge Base Tables
-- ============================================================

CREATE TABLE kb_document (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL,
    content     TEXT NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id   VARCHAR(50),
    category    VARCHAR(50),
    chunk_count INT DEFAULT 0,
    status      TINYINT DEFAULT 1,
    created_by  BIGINT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_source (source_type, source_id),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE kb_vector_mapping (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    chunk_text  TEXT NOT NULL,
    milvus_id   VARCHAR(50) NOT NULL,
    token_count INT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_doc (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- AI Session and Memory Tables
-- ============================================================

CREATE TABLE ai_session (
    id              VARCHAR(64) PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    title           VARCHAR(200),
    work_order_id   BIGINT,
    status          TINYINT DEFAULT 1,
    total_messages  INT DEFAULT 0,
    total_tokens    BIGINT DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active_at  DATETIME,
    INDEX idx_user (user_id),
    INDEX idx_wo (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_message (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id  VARCHAR(64) NOT NULL,
    role        VARCHAR(20) NOT NULL,
    content     TEXT,
    tool_calls  JSON,
    tool_call_id VARCHAR(100),
    token_count INT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
