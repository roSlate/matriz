-- Matriz: fictional demo data, loaded only by the "dev" profile (see application-dev.yml).
-- All names, emails and tasks are invented. Every demo user has the password "matriz-demo".
-- Flyway applies this file once, so restarting the application never duplicates or wipes data.

INSERT INTO users (name, email, password_hash) VALUES
    ('Ana Silva',  'ana@example.org',   '$2b$10$pVKyG.7HgMu/ZlqXQsEtE.ncsvFRbMA9pwN5v/p3qpI/J5APOqicy'),
    ('Rui Costa',  'rui@example.org',   '$2b$10$pVKyG.7HgMu/ZlqXQsEtE.ncsvFRbMA9pwN5v/p3qpI/J5APOqicy'),
    ('Sofia Lima', 'sofia@example.org', '$2b$10$pVKyG.7HgMu/ZlqXQsEtE.ncsvFRbMA9pwN5v/p3qpI/J5APOqicy');

INSERT INTO projects (name, description, progress_override) VALUES
    ('Website redesign',  'New landing page and style guide.', NULL),
    ('Client onboarding', 'Welcome process for new clients.',  80),
    ('Team handbook',     'How the team works.',               NULL);

-- Different access for each user: Ana and Rui cannot see "Team handbook"; Sofia is a Viewer
-- in "Website redesign" and has no access to "Client onboarding".
INSERT INTO project_members (project_id, user_id, role)
SELECT p.id, u.id, m.role
FROM (VALUES
    ('Website redesign',  'ana@example.org',   'OWNER'),
    ('Website redesign',  'rui@example.org',   'MEMBER'),
    ('Website redesign',  'sofia@example.org', 'VIEWER'),
    ('Client onboarding', 'rui@example.org',   'OWNER'),
    ('Client onboarding', 'ana@example.org',   'MEMBER'),
    ('Team handbook',     'sofia@example.org', 'OWNER')
) AS m (project, email, role)
JOIN projects p ON p.name = m.project
JOIN users u ON u.email = m.email;

INSERT INTO tasks (project_id, title, description, status, priority)
SELECT p.id, t.title, t.description, t.status, t.priority
FROM (VALUES
    ('Website redesign', 'Write the project brief',      'One page, for the client.',        'IN_PROGRESS', 'HIGH'),
    ('Website redesign', 'Choose the colour palette',    NULL::text,                         'IN_PROGRESS', 'MEDIUM'),
    ('Website redesign', 'Design the landing page',      'Desktop first, then mobile.',      'TODO',        'MEDIUM'),
    ('Website redesign', 'Collect client feedback',      NULL::text,                         'TODO',        'MEDIUM'),
    ('Website redesign', 'Plan the launch',              NULL::text,                         'TODO',        'LOW'),
    ('Website redesign', 'Write the copy',               'Headlines and short sections.',    'TODO',        'HIGH'),
    ('Website redesign', 'Set up the style guide',       NULL::text,                         'DONE',        'LOW'),
    ('Website redesign', 'Audit the current site',       NULL::text,                         'DONE',        'MEDIUM'),
    ('Website redesign', 'Interview stakeholders',       NULL::text,                         'DONE',        'HIGH'),
    ('Website redesign', 'Pick the hosting plan',        NULL::text,                         'DONE',        'LOW'),
    ('Website redesign', 'Create the sitemap',           NULL::text,                         'DONE',        'MEDIUM'),
    ('Website redesign', 'Draft the content outline',    NULL::text,                         'DONE',        'MEDIUM'),
    ('Client onboarding', 'Prepare the welcome email',   NULL::text,                         'TODO',        'MEDIUM'),
    ('Client onboarding', 'Schedule the kickoff call',   NULL::text,                         'TODO',        'HIGH'),
    ('Client onboarding', 'Collect access details',      NULL::text,                         'TODO',        'MEDIUM'),
    ('Client onboarding', 'Write the onboarding checklist', NULL::text,                      'IN_PROGRESS', 'HIGH'),
    ('Client onboarding', 'Create the client folder',    NULL::text,                         'DONE',        'LOW'),
    ('Client onboarding', 'Send the contract',           NULL::text,                         'DONE',        'HIGH')
) AS t (project, title, description, status, priority)
JOIN projects p ON p.name = t.project;

-- Assignments refer to memberships, so they can only name members of the task's project.
INSERT INTO task_assignments (task_id, user_id, project_id)
SELECT t.id, u.id, t.project_id
FROM (VALUES
    ('Write the project brief', 'ana@example.org'),
    ('Write the project brief', 'rui@example.org'),
    ('Collect client feedback', 'rui@example.org'),
    ('Set up the style guide',  'ana@example.org')
) AS a (title, email)
JOIN tasks t ON t.title = a.title
JOIN users u ON u.email = a.email;

INSERT INTO comments (task_id, author_id, content, created_at)
SELECT t.id, u.id, c.content, c.created_at::timestamptz
FROM (VALUES
    ('Write the project brief', 'ana@example.org', 'Draft is ready for review.',          '2026-11-14 10:15:00+00'),
    ('Write the project brief', 'rui@example.org', 'Added the timeline section.',         '2026-11-14 15:40:00+00'),
    ('Write the project brief', 'ana@example.org', 'Waiting for the client''s feedback.', '2026-11-15 09:05:00+00')
) AS c (title, email, content, created_at)
JOIN tasks t ON t.title = c.title
JOIN users u ON u.email = c.email;
