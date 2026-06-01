-- =============================================================
-- ClearDocs — Initial Schema (PostgreSQL)
-- =============================================================

-- Sequences
CREATE SEQUENCE IF NOT EXISTS user_sequence START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS document_sequence START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS workflow_sequence START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS step_sequence START 1 INCREMENT 1;

-- Roles
CREATE TABLE IF NOT EXISTS roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

-- Users
CREATE TABLE IF NOT EXISTS users (
    id                 BIGINT       DEFAULT nextval('user_sequence') PRIMARY KEY,
    username           VARCHAR(50)  NOT NULL UNIQUE,
    email              VARCHAR(100) NOT NULL UNIQUE,
    password           VARCHAR(255) NOT NULL,
    full_name          VARCHAR(100),
    department         VARCHAR(100),
    enabled            BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN      NOT NULL DEFAULT TRUE,
    oauth_provider     VARCHAR(50),
    oauth_id           VARCHAR(255),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version            BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_users_email    ON users (email);
CREATE INDEX idx_users_username ON users (username);

-- User-Role join
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

-- Documents
CREATE TABLE IF NOT EXISTS documents (
    id               BIGINT        DEFAULT nextval('document_sequence') PRIMARY KEY,
    reference_number VARCHAR(30)   NOT NULL UNIQUE,
    title            VARCHAR(255)  NOT NULL,
    description      TEXT,
    file_path        VARCHAR(512),
    file_name        VARCHAR(255),
    file_size        BIGINT,
    content_type     VARCHAR(100),
    state            VARCHAR(30)   NOT NULL DEFAULT 'DRAFT',
    submitter_id     BIGINT        NOT NULL REFERENCES users(id),
    document_type    VARCHAR(50),
    priority         VARCHAR(20)   NOT NULL DEFAULT 'NORMAL',
    rejection_reason TEXT,
    approved_at      TIMESTAMPTZ,
    rejected_at      TIMESTAMPTZ,
    submitted_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version          BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_doc_submitter  ON documents (submitter_id);
CREATE INDEX idx_doc_state      ON documents (state);
CREATE INDEX idx_doc_created    ON documents (created_at DESC);
CREATE INDEX idx_doc_type       ON documents (document_type);
CREATE INDEX idx_doc_priority   ON documents (priority);

-- Approval Workflows
CREATE TABLE IF NOT EXISTS approval_workflows (
    id           BIGINT       DEFAULT nextval('workflow_sequence') PRIMARY KEY,
    document_id  BIGINT       NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    status       VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    current_step INT          NOT NULL DEFAULT 0,
    completed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version      BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_wf_document ON approval_workflows (document_id);
CREATE INDEX idx_wf_status   ON approval_workflows (status);

-- Workflow Steps
CREATE TABLE IF NOT EXISTS workflow_steps (
    id           BIGINT       DEFAULT nextval('step_sequence') PRIMARY KEY,
    workflow_id  BIGINT       NOT NULL REFERENCES approval_workflows(id) ON DELETE CASCADE,
    step_order   INT          NOT NULL,
    reviewer_id  BIGINT       REFERENCES users(id),
    reviewer_role VARCHAR(50),
    status       VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    comments     TEXT,
    action_taken VARCHAR(30),
    due_date     TIMESTAMPTZ,
    acted_at     TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_step_workflow ON workflow_steps (workflow_id);
CREATE INDEX idx_step_reviewer ON workflow_steps (reviewer_id);
CREATE INDEX idx_step_status   ON workflow_steps (status);

-- Seed roles
INSERT INTO roles (name) VALUES
    ('ROLE_USER'),
    ('ROLE_REVIEWER'),
    ('ROLE_APPROVER'),
    ('ROLE_ADMIN'),
    ('ROLE_AUDITOR')
ON CONFLICT (name) DO NOTHING;
