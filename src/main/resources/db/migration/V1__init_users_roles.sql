CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    phone         VARCHAR(32),
    password_hash VARCHAR(100) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    company_id    BIGINT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id),
    role_id BIGINT NOT NULL REFERENCES roles (id),
    PRIMARY KEY (user_id, role_id)
);

INSERT INTO roles (code)
VALUES ('PLATFORM_ADMIN'),
       ('BUSINESS_OWNER'),
       ('STAFF'),
       ('CLIENT');

-- Dev seed: admin@rezerv.bg / admin123
INSERT INTO users (email, password_hash, first_name, last_name, status)
VALUES ('admin@rezerv.bg',
        '$2y$10$eoUOXzcKnwOf1ZGV7Pm4S.zMEMtvSZmh/bG2dcVyQ2kWRUvHWQgf6',
        'Platform', 'Admin', 'ACTIVE');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@rezerv.bg' AND r.code = 'PLATFORM_ADMIN';
