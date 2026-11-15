package com.matriz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class SchemaRulesTest extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void emptyTables() {
        jdbc.execute("TRUNCATE comments, task_assignments, tasks, project_members, projects, users "
                + "RESTART IDENTITY CASCADE");
    }

    @Test
    void migrationsCreateAllTables() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);

        assertThat(tables).contains("users", "projects", "project_members", "tasks",
                "task_assignments", "comments");
    }

    @Test
    void emailIsUniqueIgnoringLetterCase() {
        user("ana@example.org");

        assertThrows(DataIntegrityViolationException.class, () -> user("ANA@Example.org"));
    }

    @Test
    void aProjectCannotHaveTwoOwners() {
        long project = project("Website redesign");
        member(project, user("ana@example.org"), "OWNER");
        long rui = user("rui@example.org");

        assertThrows(DataIntegrityViolationException.class, () -> member(project, rui, "OWNER"));
    }

    @Test
    void titleMustHaveBetweenThreeAndHundredFiftyCharactersAfterTrimming() {
        long project = project("Website redesign");

        assertThrows(DataIntegrityViolationException.class, () -> task(project, "   "));
        assertThrows(DataIntegrityViolationException.class, () -> task(project, "ab"));
        assertThrows(DataIntegrityViolationException.class, () -> task(project, "  ab  "));
        assertThrows(DataIntegrityViolationException.class, () -> task(project, "x".repeat(151)));
        task(project, "abc");
        task(project, "x".repeat(150));
    }

    @Test
    void statusAndPriorityDefaultAndRejectOtherValues() {
        long project = project("Website redesign");
        long task = task(project, "Write the brief");

        assertThat(jdbc.queryForObject("SELECT status FROM tasks WHERE id = ?", String.class, task))
                .isEqualTo("TODO");
        assertThat(jdbc.queryForObject("SELECT priority FROM tasks WHERE id = ?", String.class, task))
                .isEqualTo("MEDIUM");
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE tasks SET status = 'BLOCKED' WHERE id = ?", task));
    }

    @Test
    void aTaskCanOnlyBeAssignedToAMemberOfItsProject() {
        long project = project("Website redesign");
        long task = task(project, "Write the brief");
        long outsider = user("sofia@example.org");

        assertThrows(DataIntegrityViolationException.class, () -> assign(task, outsider, project));
    }

    @Test
    void removingAMemberRemovesTheirAssignmentsButKeepsTheTask() {
        long project = project("Website redesign");
        long ana = user("ana@example.org");
        long rui = user("rui@example.org");
        member(project, ana, "OWNER");
        member(project, rui, "MEMBER");
        long task = task(project, "Write the brief");
        assign(task, ana, project);
        assign(task, rui, project);

        jdbc.update("DELETE FROM project_members WHERE project_id = ? AND user_id = ?", project, rui);

        assertThat(count("task_assignments")).isEqualTo(1);
        assertThat(count("tasks")).isEqualTo(1);
    }

    @Test
    void deletingAProjectRemovesEverythingInsideItButNotTheUsers() {
        long project = project("Website redesign");
        long ana = user("ana@example.org");
        member(project, ana, "OWNER");
        long task = task(project, "Write the brief");
        assign(task, ana, project);
        jdbc.update("INSERT INTO comments (task_id, author_id, content) VALUES (?, ?, 'Draft ready')",
                task, ana);

        jdbc.update("DELETE FROM projects WHERE id = ?", project);

        assertThat(count("project_members")).isZero();
        assertThat(count("tasks")).isZero();
        assertThat(count("task_assignments")).isZero();
        assertThat(count("comments")).isZero();
        assertThat(count("users")).isEqualTo(1);
    }

    private long user(String email) {
        return jdbc.queryForObject(
                "INSERT INTO users (name, email, password_hash) VALUES ('Test user', ?, 'not-a-real-hash') RETURNING id",
                Long.class, email);
    }

    private long project(String name) {
        return jdbc.queryForObject("INSERT INTO projects (name) VALUES (?) RETURNING id", Long.class, name);
    }

    private void member(long project, long user, String role) {
        jdbc.update("INSERT INTO project_members (project_id, user_id, role) VALUES (?, ?, ?)",
                project, user, role);
    }

    private long task(long project, String title) {
        return jdbc.queryForObject("INSERT INTO tasks (project_id, title) VALUES (?, ?) RETURNING id",
                Long.class, project, title);
    }

    private void assign(long task, long user, long project) {
        jdbc.update("INSERT INTO task_assignments (task_id, user_id, project_id) VALUES (?, ?, ?)",
                task, user, project);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }
}
