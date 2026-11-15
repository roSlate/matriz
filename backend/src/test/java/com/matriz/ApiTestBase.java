package com.matriz;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.matriz.identity.User;
import com.matriz.identity.UserPrincipal;
import com.matriz.identity.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

// Shared setup for the API tests: three users, an empty database, and helpers to act as a user
// and to create fixtures directly in the database. Subclasses add @SpringBootTest and @AutoConfigureMockMvc.
abstract class ApiTestBase extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    JdbcTemplate jdbc;

    User ana;
    User rui;
    User sofia;

    @BeforeEach
    void threeUsers() {
        jdbc.execute("TRUNCATE comments, task_assignments, tasks, project_members, projects, users "
                + "RESTART IDENTITY CASCADE");
        ana = users.save(new User("Ana Silva", "ana@example.org", "not-a-real-hash"));
        rui = users.save(new User("Rui Costa", "rui@example.org", "not-a-real-hash"));
        sofia = users.save(new User("Sofia Lima", "sofia@example.org", "not-a-real-hash"));
    }

    static RequestPostProcessor as(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    ResultActions send(User user, MockHttpServletRequestBuilder request, String json) throws Exception {
        return mvc.perform(request.with(as(user)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    long project(User owner, String name, int daysAgo) {
        long id = jdbc.queryForObject(
                "INSERT INTO projects (name, created_at, updated_at) VALUES (?, now() - ? * interval '1 day', now()) RETURNING id",
                Long.class, name, daysAgo);
        member(id, owner, "OWNER");
        return id;
    }

    void member(long projectId, User user, String role) {
        jdbc.update("INSERT INTO project_members (project_id, user_id, role) VALUES (?, ?, ?)",
                projectId, user.getId(), role);
    }

    long task(long projectId, String title, String status, String priority, int minutesAgo) {
        return jdbc.queryForObject(
                "INSERT INTO tasks (project_id, title, status, priority, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, now() - ? * interval '1 minute', now()) RETURNING id",
                Long.class, projectId, title, status, priority, minutesAgo);
    }

    void assign(long taskId, User user, long projectId) {
        jdbc.update("INSERT INTO task_assignments (task_id, user_id, project_id) VALUES (?, ?, ?)",
                taskId, user.getId(), projectId);
    }

    int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }
}
