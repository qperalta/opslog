/* V17: the AI assistant's latest summary of the member's plan, one row per member (set_coach_summary replaces it) */
CREATE TABLE coach_summaries(
    user_id     BIGINT         PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    summary     VARCHAR(1000)  NOT NULL,
    updated_at  timestamptz    NOT NULL DEFAULT now(),

    CONSTRAINT ck_coach_summaries_summary CHECK (length(trim(summary)) > 0)
);
