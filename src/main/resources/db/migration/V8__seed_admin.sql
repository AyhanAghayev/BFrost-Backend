INSERT INTO users (username, email, password_hash, display_name, role, is_verified)
VALUES (
    'admin',
    'admin@bfrost.com',
    '$2a$10$w0uTMc6UGjc2mTu9iW3as.tToefPDah34aSQxZrNe8Ghr3nWx9zGe',
    'Admin',
    'ADMIN',
    TRUE
)
ON CONFLICT DO NOTHING;
