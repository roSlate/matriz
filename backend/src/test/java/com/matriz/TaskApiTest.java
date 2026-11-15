package com.matriz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.matriz.identity.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class TaskApiTest extends ApiTestBase {

    long website;   // Ana owner, Rui member, Sofia viewer
    long handbook;  // Sofia owner; Ana and Rui have no access

    @BeforeEach
    void twoProjects() {
        website = project(ana, "Website redesign", 2);
        member(website, rui, "MEMBER");
        member(website, sofia, "VIEWER");
        handbook = project(sofia, "Team handbook", 1);
    }

    // ---- creating and reading back (AC-04.1, AC-04.4) ----

    @Test
    void aCreatedTaskIsStoredInTheDatabaseAndCanBeReadBack() throws Exception {
        String created = createTask(ana, website, "{\"title\":\"  Write the brief  \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Write the brief"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.assignees", hasSize(0)))
                .andExpect(jsonPath("$.projectId").value(website))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(created, "$.id")).longValue();

        assertThat(jdbc.queryForObject("SELECT title || '/' || status || '/' || priority FROM tasks WHERE id = ?",
                String.class, id)).isEqualTo("Write the brief/TODO/MEDIUM");

        mvc.perform(get("/api/v1/tasks/" + id).with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Write the brief"));
        mvc.perform(get("/api/v1/projects/" + website + "/tasks").with(as(rui)))
                .andExpect(jsonPath("$.items[0].id").value(id))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void theCreatorMayChooseStatusAndPriority() throws Exception {
        createTask(rui, website, "{\"title\":\"Plan the launch\",\"status\":\"IN_PROGRESS\",\"priority\":\"HIGH\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void titleAndDescriptionAreCheckedAfterTrimming() throws Exception {
        for (String invalidTitle : List.of("", "   ", "ab", "  ab  ", "x".repeat(151))) {
            createTask(ana, website, "{\"title\":\"" + invalidTitle + "\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("title"));
        }
        createTask(ana, website, "{\"title\":\"Valid title\",\"description\":\"" + "x".repeat(2001) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("description"));
        assertThat(count("tasks")).isZero();

        createTask(ana, website, "{\"title\":\"abc\"}").andExpect(status().isCreated());
        createTask(ana, website, "{\"title\":\"" + "x".repeat(150) + "\",\"description\":\"" + "x".repeat(2000) + "\"}")
                .andExpect(status().isCreated());
        assertThat(count("tasks")).isEqualTo(2);
    }

    @Test
    void anUnknownFieldOrAnInvalidStatusIsRefused() throws Exception {
        createTask(ana, website, "{\"title\":\"Valid title\",\"projectId\":" + handbook + "}")
                .andExpect(status().isBadRequest());
        createTask(ana, website, "{\"title\":\"Valid title\",\"status\":\"BLOCKED\"}").andExpect(status().isBadRequest());
        createTask(ana, website, "{\"title\":\"Valid title\",\"priority\":\"URGENT\"}").andExpect(status().isBadRequest());
        assertThat(count("tasks")).isZero();
    }

    // ---- who may do what (the permission matrix) ----

    @Test
    void aViewerCanReadButNotChangeAnything() throws Exception {
        long existing = task(website, "Existing task", "TODO", "MEDIUM", 5);

        mvc.perform(get("/api/v1/projects/" + website + "/tasks").with(as(sofia))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/tasks/" + existing).with(as(sofia))).andExpect(status().isOk());

        createTask(sofia, website, "{\"title\":\"Not allowed\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_INSUFFICIENT"));
        send(sofia, patch("/api/v1/tasks/" + existing), "{\"status\":\"DONE\"}").andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/tasks/" + existing).with(as(sofia)).with(csrf()))
                .andExpect(status().isForbidden());
        // The input is checked before the role: a Viewer sending nonsense is told so.
        createTask(sofia, website, "{\"title\":\"x\"}").andExpect(status().isBadRequest());

        assertThat(count("tasks")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM tasks WHERE id = ?", String.class, existing))
                .isEqualTo("TODO");
    }

    @Test
    void aMemberCanCreateEditAndDelete() throws Exception {
        long id = ((Number) JsonPath.read(createTask(rui, website, "{\"title\":\"Member task\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id")).longValue();

        send(rui, patch("/api/v1/tasks/" + id), "{\"priority\":\"HIGH\"}").andExpect(status().isOk());
        mvc.perform(delete("/api/v1/tasks/" + id).with(as(rui)).with(csrf())).andExpect(status().isNoContent());
        assertThat(count("tasks")).isZero();
    }

    @Test
    void aNonMemberGets404ForEverythingEvenWithAnInvalidBody() throws Exception {
        long secret = task(handbook, "Private task", "TODO", "HIGH", 5);

        // by project
        mvc.perform(get("/api/v1/projects/" + handbook + "/tasks").with(as(rui))).andExpect(status().isNotFound());
        createTask(rui, handbook, "{\"title\":\"x\"}").andExpect(status().isNotFound());
        createTask(rui, handbook, "{ this is not json").andExpect(status().isNotFound());
        // by task identifier: changing the identifier does not reach another project's task
        mvc.perform(get("/api/v1/tasks/" + secret).with(as(rui))).andExpect(status().isNotFound());
        send(rui, patch("/api/v1/tasks/" + secret), "{\"title\":\"x\"}").andExpect(status().isNotFound());
        send(rui, patch("/api/v1/tasks/" + secret), "{\"status\":\"DONE\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Resource not found."));
        mvc.perform(delete("/api/v1/tasks/" + secret).with(as(rui)).with(csrf())).andExpect(status().isNotFound());
        // a task that does not exist looks exactly the same
        mvc.perform(get("/api/v1/tasks/999999").with(as(rui)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Resource not found."));

        assertThat(jdbc.queryForObject("SELECT status FROM tasks WHERE id = ?", String.class, secret)).isEqualTo("TODO");
        assertThat(count("tasks")).isEqualTo(1);
    }

    @Test
    void aMalformedTaskIdIsABadRequest() throws Exception {
        mvc.perform(get("/api/v1/tasks/abc").with(as(ana))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/tasks/0").with(as(ana))).andExpect(status().isBadRequest());
    }

    // ---- listing, filters and paging (AC-09.2 to AC-09.4) ----

    @Test
    void filtersByStatusAndPriorityCombineAndNoMatchIsNotAnError() throws Exception {
        task(website, "Task A", "TODO", "HIGH", 40);
        task(website, "Task B", "TODO", "LOW", 30);
        task(website, "Task C", "DONE", "HIGH", 20);
        task(website, "Task D", "IN_PROGRESS", "MEDIUM", 10);
        task(handbook, "Other project task", "TODO", "HIGH", 5);
        String base = "/api/v1/projects/" + website + "/tasks";

        mvc.perform(get(base).with(as(ana))).andExpect(jsonPath("$.totalItems").value(4));
        mvc.perform(get(base + "?status=TODO").with(as(ana))).andExpect(jsonPath("$.totalItems").value(2));
        mvc.perform(get(base + "?priority=HIGH").with(as(ana))).andExpect(jsonPath("$.totalItems").value(2));
        mvc.perform(get(base + "?status=TODO&priority=HIGH").with(as(ana)))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Task A"));
        mvc.perform(get(base + "?status=DONE&priority=LOW").with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(get(base + "?status=BLOCKED").with(as(ana))).andExpect(status().isBadRequest());
        mvc.perform(get(base + "?priority=URGENT").with(as(ana))).andExpect(status().isBadRequest());
    }

    @Test
    void theListIsNewestFirstAndPaged() throws Exception {
        task(website, "Oldest", "TODO", "MEDIUM", 40);
        task(website, "Second", "TODO", "MEDIUM", 30);
        task(website, "Third", "TODO", "MEDIUM", 20);
        task(website, "Newest", "TODO", "MEDIUM", 10);
        String base = "/api/v1/projects/" + website + "/tasks";

        mvc.perform(get(base + "?size=3").with(as(ana)))
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].title").value("Newest"))
                .andExpect(jsonPath("$.items[2].title").value("Second"))
                .andExpect(jsonPath("$.totalItems").value(4))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get(base + "?size=3&page=1").with(as(ana)))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("Oldest"));
        for (String bad : List.of("size=0", "size=101", "page=-1")) {
            mvc.perform(get(base + "?" + bad).with(as(ana))).andExpect(status().isBadRequest());
        }
    }

    @Test
    void assigneesAreShownWithTheirNamesAndAnUnassignedTaskHasNone() throws Exception {
        long shared = task(website, "Shared task", "TODO", "MEDIUM", 20);
        long alone = task(website, "Unassigned task", "TODO", "MEDIUM", 10);
        assign(shared, rui, website);
        assign(shared, ana, website);

        mvc.perform(get("/api/v1/tasks/" + shared).with(as(sofia)))
                .andExpect(jsonPath("$.assignees", hasSize(2)))
                .andExpect(jsonPath("$.assignees[0].name").value("Ana Silva"))
                .andExpect(jsonPath("$.assignees[1].name").value("Rui Costa"));
        mvc.perform(get("/api/v1/tasks/" + alone).with(as(sofia)))
                .andExpect(jsonPath("$.assignees", hasSize(0)));
        mvc.perform(get("/api/v1/projects/" + website + "/tasks").with(as(ana)))
                .andExpect(jsonPath("$.items[1].assignees", hasSize(2)));
    }

    // ---- editing (AC-05.1 to AC-05.3) ----

    @Test
    void statusAndPriorityChangeAndADoneTaskCanBeReopened() throws Exception {
        long id = task(website, "Existing task", "TODO", "MEDIUM", 5);
        String path = "/api/v1/tasks/" + id;

        send(ana, patch(path), "{\"status\":\"DONE\",\"priority\":\"LOW\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.priority").value("LOW"));
        send(ana, patch(path), "{\"status\":\"IN_PROGRESS\"}")
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("LOW"));
        send(ana, patch(path), "{\"status\":\"TODO\"}").andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void onlyTheGivenFieldsChangeAndAnEmptyDescriptionClearsIt() throws Exception {
        long id = task(website, "Original title", "TODO", "MEDIUM", 5);
        jdbc.update("UPDATE tasks SET description = 'Old description' WHERE id = ?", id);
        String path = "/api/v1/tasks/" + id;

        send(ana, patch(path), "{\"title\":\"  New title  \"}")
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.description").value("Old description"));
        send(ana, patch(path), "{\"description\":\"\"}").andExpect(jsonPath("$.description").value(nullValue()));
        assertThat(jdbc.queryForObject("SELECT description FROM tasks WHERE id = ?", String.class, id)).isNull();
    }

    @Test
    void anInvalidEditChangesNothing() throws Exception {
        long id = task(website, "Original title", "TODO", "MEDIUM", 5);
        String path = "/api/v1/tasks/" + id;

        send(ana, patch(path), "{}").andExpect(status().isBadRequest());
        send(ana, patch(path), "{\"title\":\"  ab  \"}").andExpect(status().isBadRequest());
        send(ana, patch(path), "{\"description\":\"" + "x".repeat(2001) + "\"}").andExpect(status().isBadRequest());
        send(ana, patch(path), "{\"status\":\"BLOCKED\"}").andExpect(status().isBadRequest());
        send(ana, patch(path), "{\"title\":\"Valid title\",\"projectId\":" + handbook + "}")
                .andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("SELECT title FROM tasks WHERE id = ?", String.class, id))
                .isEqualTo("Original title");
        assertThat(jdbc.queryForObject("SELECT project_id FROM tasks WHERE id = ?", Long.class, id)).isEqualTo(website);
    }

    // ---- deleting (AC-05.4) ----

    @Test
    void deletingATaskRemovesItsAssignmentsAndCommentsButNotOtherTasks() throws Exception {
        long doomed = task(website, "Doomed task", "TODO", "MEDIUM", 20);
        long kept = task(website, "Kept task", "TODO", "MEDIUM", 10);
        assign(doomed, ana, website);
        assign(kept, rui, website);
        jdbc.update("INSERT INTO comments (task_id, author_id, content) VALUES (?, ?, 'A comment')", doomed, ana.getId());
        jdbc.update("INSERT INTO comments (task_id, author_id, content) VALUES (?, ?, 'Another')", kept, rui.getId());

        mvc.perform(delete("/api/v1/tasks/" + doomed).with(as(rui)).with(csrf())).andExpect(status().isNoContent());

        assertThat(count("tasks")).isEqualTo(1);
        assertThat(count("task_assignments")).isEqualTo(1);
        assertThat(count("comments")).isEqualTo(1);
        mvc.perform(get("/api/v1/tasks/" + doomed).with(as(ana))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/tasks/" + kept).with(as(ana))).andExpect(status().isOk());
    }

    // ---- counts for the dashboard (FR-23, FR-24) ----

    @Test
    void projectCountsAndProgressFollowTheTasks() throws Exception {
        task(website, "One", "TODO", "MEDIUM", 40);
        task(website, "Two", "TODO", "MEDIUM", 30);
        long third = task(website, "Three", "IN_PROGRESS", "MEDIUM", 20);
        task(website, "Four", "DONE", "MEDIUM", 10);
        task(handbook, "Other project", "DONE", "MEDIUM", 5);

        mvc.perform(get("/api/v1/projects/" + website).with(as(ana)))
                .andExpect(jsonPath("$.taskCounts.todo").value(2))
                .andExpect(jsonPath("$.taskCounts.inProgress").value(1))
                .andExpect(jsonPath("$.taskCounts.done").value(1))
                .andExpect(jsonPath("$.taskCounts.total").value(4))
                .andExpect(jsonPath("$.progress.computed").value(25));

        send(ana, patch("/api/v1/tasks/" + third), "{\"status\":\"DONE\"}").andExpect(status().isOk());

        mvc.perform(get("/api/v1/projects").with(as(ana)))
                .andExpect(jsonPath("$.items[?(@.id==" + website + ")].taskCounts.done").value(2))
                .andExpect(jsonPath("$.items[?(@.id==" + website + ")].taskCounts.inProgress").value(0))
                .andExpect(jsonPath("$.items[?(@.id==" + website + ")].progress.computed").value(50));
    }

    // ---- helpers ----

    private ResultActions createTask(User user, long projectId, String json) throws Exception {
        return send(user, post("/api/v1/projects/" + projectId + "/tasks"), json);
    }
}
