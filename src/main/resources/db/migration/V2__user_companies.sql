-- Membership: един user може да е owner на много фирми.
-- users.company_id остава „активната“ фирма (JWT claim / X-Company-Id).

CREATE TABLE user_companies (
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    company_id BIGINT      NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, company_id)
);

CREATE INDEX idx_user_companies_company ON user_companies (company_id);

-- Backfill от старото 1:1 поле.
INSERT INTO user_companies (user_id, company_id)
SELECT id, company_id
FROM users
WHERE company_id IS NOT NULL;
