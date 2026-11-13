/* V19: what's new in OpsLog, shown under the avatar; a member's read marker says which ones they have seen */
CREATE TABLE announcements(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title           VARCHAR(80)   NOT NULL,
    body            VARCHAR(500)  NOT NULL,
    published_at    timestamptz   NOT NULL DEFAULT now()
);

CREATE INDEX ix_announcements_published ON announcements (published_at DESC);

CREATE TABLE notification_reads(
    user_id         BIGINT        PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    read_at         timestamptz   NOT NULL
);

INSERT INTO announcements (title, body) VALUES
    ('Add exercises mid-workout',
     'During a workout, tap + between two exercises to add one right there, with its sets and weight. '
         || 'Type a name that is not in the list and OpsLog adds it as your own exercise.');
