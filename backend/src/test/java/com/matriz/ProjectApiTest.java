package com.matriz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matriz.identity.User;
import com.matriz.identity.UserPrincipal;
import com.matriz.identity.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectApiTest extends IntegrationTestBase {

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

    // ---- creating (AC-02.1 to AC-02.3) ----

    @Test
    void creatingAProjectMakesTheCallerItsOwnerAndStoresTrimmedText() throws Exception {
        create(ana, "  Website redesign  ", "   ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Website redesign"))
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.taskCounts.total").value(0))
                .andExpect(jsonPath("$.progress.computed").value(0))
                .andExpect(jsonPath("$.progress.manual").value(nullValue()))
                .andExpect(jsonPath("$.progress.effective").value(0));

        assertThat(jdbc.queryForObject("SELECT role FROM project_members WHERE user_id = ?", String.class, ana.getId()))
                .isEqualTo("OWNER");
        assertThat(jdbc.queryForObject("SELECT name FROM projects", String.class)).isEqualTo("Website redesign");
        assertThat(jdbc.queryForObject("SELECT description FROM projects", String.class)).isNull();
    }

    @Test
    void aClientCannotChooseTheOwnerOrSendAnyOtherUnknownField() throws Exception {
        send(ana, post("/api/v1/projects"), "{\"name\":\"Website redesign\",\"ownerId\":" + rui.getId() + "}")
                .andExpect(status().isBadRequest());

        assertThat(count("projects")).isZero();
    }

    @Test
    void nameAndDescriptionAreCheckedAfterTrimming() throws Exception {
        for (String invalidName : List.of("", "   ", "ab", "  ab  ", "x".repeat(101))) {
            create(ana, invalidName, null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("name"));
        }
        create(ana, "Website redesign", "x".repeat(1001))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("description"));

        assertThat(count("projects")).isZero();

        create(ana, "abc", null).andExpect(status().isCreated());                      // lower boundary
        create(ana, "x".repeat(100), "x".repeat(1000)).andExpect(status().isCreated()); // upper boundaries
        assertThat(count("projects")).isEqualTo(2);
    }

    // ---- listing (AC-06.1, pagination) ----

    @Test
    void listShowsOnlyTheCallersProjectsNewestFirstWithTheirRole() throws Exception {
        long website = project(ana, "Website redesign", 3);
        long onboarding = project(rui, "Client onboarding", 1);
        project(sofia, "Team handbook", 2);
        member(onboarding, ana, "MEMBER");

        mvc.perform(get("/api/v1/projects").with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].name").value("Client onboarding"))
                .andExpect(jsonPath("$.items[0].myRole").value("MEMBER"))
                .andExpect(jsonPath("$.items[1].name").value("Website redesign"))
                .andExpect(jsonPath("$.items[1].myRole").value("OWNER"))
                .andExpect(jsonPath("$.items[1].id").value(website));
    }

    @Test
    void aUserWithoutProjectsGetsAnEmptyListNotAnError() throws Exception {
        project(sofia, "Team handbook", 1);

        mvc.perform(get("/api/v1/projects").with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void paginationSplitsTheListAndRejectsOutOfRangeValues() throws Exception {
        project(ana, "Project one", 3);
        project(ana, "Project two", 2);
        project(ana, "Project three", 1);

        mvc.perform(get("/api/v1/projects?size=2").with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items[0].name").value("Project three"));
        mvc.perform(get("/api/v1/projects?page=1&size=2").with(as(ana)))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].name").value("Project one"));

        for (String bad : List.of("size=0", "size=101", "page=-1")) {
            mvc.perform(get("/api/v1/projects?" + bad).with(as(ana))).andExpect(status().isBadRequest());
        }
    }

    // ---- the access step and the order of checks (CC-05) ----

    @Test
    void aNonMemberGets404ForEverythingEvenWithAnInvalidBody() throws Exception {
        long handbook = project(sofia, "Team handbook", 1);
        String path = "/api/v1/projects/" + handbook;

        mvc.perform(get(path).with(as(rui))).andExpect(status().isNotFound());
        send(rui, patch(path), "{\"name\":\"x\"}").andExpect(status().isNotFound());
        send(rui, patch(path), "{ this is not json").andExpect(status().isNotFound());
        send(rui, patch(path), "{\"name\":\"Valid new name\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Resource not found."));
        mvc.perform(get("/api/v1/projects/999999").with(as(rui)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Resource not found."));
    }

    @Test
    void aMalformedProjectIdIsABadRequest() throws Exception {
        mvc.perform(get("/api/v1/projects/abc").with(as(ana))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/projects/0").with(as(ana))).andExpect(status().isBadRequest());
    }

    // ---- viewing and editing (AC-02.4, AC-02.5) ----

    @Test
    void getReturnsTheCallersOwnRole() throws Exception {
        long id = project(ana, "Website redesign", 1);
        member(id, rui, "MEMBER");
        member(id, sofia, "VIEWER");

        mvc.perform(get("/api/v1/projects/" + id).with(as(ana))).andExpect(jsonPath("$.myRole").value("OWNER"));
        mvc.perform(get("/api/v1/projects/" + id).with(as(rui))).andExpect(jsonPath("$.myRole").value("MEMBER"));
        mvc.perform(get("/api/v1/projects/" + id).with(as(sofia))).andExpect(jsonPath("$.myRole").value("VIEWER"));
    }

    @Test
    void theOwnerCanEditAndOnlyTheGivenFieldsChange() throws Exception {
        long id = project(ana, "Website redesign", 1);
        jdbc.update("UPDATE projects SET description = 'Old description' WHERE id = ?", id);
        String path = "/api/v1/projects/" + id;

        send(ana, patch(path), "{\"name\":\"  New name  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New name"))
                .andExpect(jsonPath("$.description").value("Old description"));

        send(ana, patch(path), "{\"description\":\"New description\"}")
                .andExpect(jsonPath("$.name").value("New name"))
                .andExpect(jsonPath("$.description").value("New description"));

        send(ana, patch(path), "{\"description\":\"\"}")
                .andExpect(jsonPath("$.description").value(nullValue()));
        assertThat(jdbc.queryForObject("SELECT description FROM projects WHERE id = ?", String.class, id)).isNull();
    }

    @Test
    void anEditWithNothingToChangeOrAnInvalidValueIsRejectedAndChangesNothing() throws Exception {
        long id = project(ana, "Website redesign", 1);
        String path = "/api/v1/projects/" + id;

        send(ana, patch(path), "{}").andExpect(status().isBadRequest());
        send(ana, patch(path), "{\"name\":\"  ab  \"}").andExpect(status().isBadRequest());
        send(ana, patch(path), "{\"description\":\"" + "x".repeat(1001) + "\"}").andExpect(status().isBadRequest());
        send(ana, patch(path), "{\"name\":\"Valid name\",\"progressOverride\":5}").andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("SELECT name FROM projects WHERE id = ?", String.class, id))
                .isEqualTo("Website redesign");
    }

    @Test
    void aMemberAndAViewerCannotEditTheProject() throws Exception {
        long id = project(ana, "Website redesign", 1);
        member(id, rui, "MEMBER");
        member(id, sofia, "VIEWER");
        String path = "/api/v1/projects/" + id;

        for (User user : List.of(rui, sofia)) {
            send(user, patch(path), "{\"name\":\"Changed by someone else\"}")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ROLE_INSUFFICIENT"));
        }
        // The body is checked before the role: a Viewer sending nonsense is told so (they have access anyway).
        send(sofia, patch(path), "{\"name\":\"x\"}").andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("SELECT name FROM projects WHERE id = ?", String.class, id))
                .isEqualTo("Website redesign");
    }

    // ---- helpers ----

    private ResultActions create(User user, String name, String description) throws Exception {
        String json = "{\"name\":" + quote(name) + (description == null ? "" : ",\"description\":" + quote(description)) + "}";
        return send(user, post("/api/v1/projects"), json);
    }

    private ResultActions send(User user,
                               org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               String json) throws Exception {
        return mvc.perform(request.with(as(user)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String quote(String text) {
        return "\"" + text + "\"";
    }

    private static RequestPostProcessor as(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private long project(User owner, String name, int daysAgo) {
        long id = jdbc.queryForObject(
                "INSERT INTO projects (name, created_at, updated_at) VALUES (?, now() - ? * interval '1 day', now()) RETURNING id",
                Long.class, name, daysAgo);
        member(id, owner, "OWNER");
        return id;
    }

    private void member(long projectId, User user, String role) {
        jdbc.update("INSERT INTO project_members (project_id, user_id, role) VALUES (?, ?, ?)",
                projectId, user.getId(), role);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }
}
