-- Seed default admin user (password: Admin@1234)
-- BCrypt hash of 'Admin@1234' with strength 12
INSERT INTO users (username, email, password, full_name, department, enabled, account_non_locked)
VALUES (
    'admin',
    'admin@cleardocs.io',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'System Administrator',
    'IT',
    TRUE,
    TRUE
) ON CONFLICT (username) DO NOTHING;

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

-- Assign AUDITOR role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_AUDITOR'
ON CONFLICT DO NOTHING;

-- Seed reviewer user (password: Reviewer@1)
INSERT INTO users (username, email, password, full_name, department, enabled, account_non_locked)
VALUES (
    'reviewer1',
    'reviewer1@cleardocs.io',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'Jane Reviewer',
    'Compliance',
    TRUE,
    TRUE
) ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'reviewer1' AND r.name = 'ROLE_REVIEWER'
ON CONFLICT DO NOTHING;

-- Seed approver user (password: Approver@1)
INSERT INTO users (username, email, password, full_name, department, enabled, account_non_locked)
VALUES (
    'approver1',
    'approver1@cleardocs.io',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'John Approver',
    'Finance',
    TRUE,
    TRUE
) ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'approver1' AND r.name = 'ROLE_APPROVER'
ON CONFLICT DO NOTHING;
