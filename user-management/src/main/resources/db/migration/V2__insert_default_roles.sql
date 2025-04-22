-- Insert default roles
INSERT INTO roles (name, description) VALUES
                                          ('ADMIN', 'Administrator with full access'),
                                          ('USER', 'Regular user with limited access'),
                                          ('MANAGER', 'Manager with intermediate access');

-- Insert default permissions
INSERT INTO permissions (name, description) VALUES
                                                ('user:read', 'Permission to read user information'),
                                                ('user:write', 'Permission to create and update users'),
                                                ('user:delete', 'Permission to delete users'),
                                                ('role:read', 'Permission to read role information'),
                                                ('role:write', 'Permission to create and update roles'),
                                                ('role:delete', 'Permission to delete roles'),
                                                ('tool:read', 'Permission to read tool information'),
                                                ('tool:write', 'Permission to create and update tools'),
                                                ('tool:delete', 'Permission to delete tools'),
                                                ('tool:execute', 'Permission to execute tools'),
                                                ('flow:read', 'Permission to read flow information'),
                                                ('flow:write', 'Permission to create and update flows'),
                                                ('flow:delete', 'Permission to delete flows'),
                                                ('flow:execute', 'Permission to execute flows');

-- Assign permissions to roles
-- ADMIN role gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'ADMIN'),
    id
FROM permissions;

-- USER role gets read and execute permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'USER'),
    id
FROM permissions
WHERE name IN ('user:read', 'tool:read', 'tool:execute', 'flow:read', 'flow:execute');

-- MANAGER role gets read, write, and execute permissions, but not delete
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE name = 'MANAGER'),
    id
FROM permissions
WHERE name NOT LIKE '%:delete';

-- Create default admin user with password 'admin'
INSERT INTO users (username, email, password, first_name, last_name, enabled, locked)
VALUES (
           'admin',
           'admin@example.com',
           -- This is bcrypt hash for password 'admin'
           '$2a$10$FLUnHYsKt0PYKUcmV3e8lOi1LlRq3G1p6duxgDUQUYcQQNWGXGwLS',
           'Admin',
           'User',
           TRUE,
           FALSE
       );

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id)
VALUES (
           (SELECT id FROM users WHERE username = 'admin'),
           (SELECT id FROM roles WHERE name = 'ADMIN')
       );