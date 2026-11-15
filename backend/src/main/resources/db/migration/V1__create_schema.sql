-- Matriz: initial schema. Derived from docs/database.md.
-- Text limits are checked on the trimmed value; the application stores trimmed values.

CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100) NOT NULL CHECK (char_length(btrim(name)) BETWEEN 1 AND 100),
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Email is unique ignoring letter case (assumption A-23).
CREATE UNIQUE INDEX users_email_lower_key ON users (lower(email));

CREATE TABLE projects (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name              VARCHAR(100) NOT NULL CHECK (char_length(btrim(name)) BETWEEN 3 AND 100),
    description       VARCHAR(1000) CHECK (description IS NULL OR char_length(btrim(description)) > 0),
    progress_override SMALLINT CHECK (progress_override BETWEEN 0 AND 100),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- The Owner is a membership row with role OWNER.
CREATE TABLE project_members (
    project_id BIGINT      NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    role       VARCHAR(10) NOT NULL CHECK (role IN ('OWNER', 'MEMBER', 'VIEWER')),
    added_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (project_id, user_id)
);

-- At most one Owner per project.
CREATE UNIQUE INDEX project_members_one_owner ON project_members (project_id) WHERE role = 'OWNER';
CREATE INDEX project_members_user_idx ON project_members (user_id);

CREATE TABLE tasks (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id  BIGINT       NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    title       VARCHAR(150) NOT NULL CHECK (char_length(btrim(title)) BETWEEN 3 AND 150),
    description VARCHAR(2000) CHECK (description IS NULL OR char_length(btrim(description)) > 0),
    status      VARCHAR(12)  NOT NULL DEFAULT 'TODO' CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    priority    VARCHAR(6)   NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- Needed by task_assignments to tie an assignment to the task's own project.
    UNIQUE (id, project_id)
);

CREATE INDEX tasks_project_status_priority_idx ON tasks (project_id, status, priority);

-- An assignment refers to a membership of the task's project: assignees must be members,
-- and removing a member removes their assignments.
CREATE TABLE task_assignments (
    task_id     BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    project_id  BIGINT      NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (task_id, user_id),
    FOREIGN KEY (task_id, project_id) REFERENCES tasks (id, project_id) ON DELETE CASCADE,
    FOREIGN KEY (project_id, user_id) REFERENCES project_members (project_id, user_id) ON DELETE CASCADE
);

CREATE INDEX task_assignments_user_idx ON task_assignments (user_id);

CREATE TABLE comments (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id    BIGINT        NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    author_id  BIGINT        NOT NULL REFERENCES users (id),
    content    VARCHAR(2000) NOT NULL CHECK (char_length(btrim(content)) BETWEEN 1 AND 2000),
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX comments_task_created_idx ON comments (task_id, created_at);
