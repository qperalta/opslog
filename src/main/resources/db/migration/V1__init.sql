/* V1: Baseline of our user table */
CREATE TABLE users(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           VARCHAR(254)     NOT NULL,
    password_hash   VARCHAR(100)    NOT NULL,
    display_name    VARCHAR(50)     NOT NULL,
    created_at      timestamptz     NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_lower CHECK (email = lower(email))
);