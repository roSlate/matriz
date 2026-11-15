package com.matriz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matriz.identity.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectSearchAndDeleteTest extends ApiTestBase {

    // ---- search (AC-09.1, AC-09.3) ----

    @Test
    void searchMatchesPartOfTheNameIgnoringCaseAmongTheCallersProjectsOnly() throws Exception {
        project(ana, "Website redesign", 3);
        project(ana, "Client onboarding", 2);
        long docs = project(rui, "Redesign of the docs", 1);
        member(docs, ana, "MEMBER");
        project(sofia, "Team handbook redesign", 1); // Ana has no access to this one

        mvc.perform(get("/api/v1/projects?search=REDESIGN").with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].name").value("Redesign of the docs"))
                .andExpect(jsonPath("$.items[1].name").value("Website redesign"));
    }

    @Test
    void theSearchTextIsTrimmedAndBlankMeansEverything() throws Exception {
        project(ana, "Website redesign", 2);
        project(ana, "Client onboarding", 1);

        search(ana, "  onboard  ")
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Client onboarding"));
        search(ana, "   ").andExpect(jsonPath("$.totalItems").value(2));
        search(ana, "").andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void percentAndUnderscoreAreMatchedAsPlainCharactersNotWildcards() throws Exception {
        project(ana, "100% done", 3);
        project(ana, "Plan A_B", 2);
        project(ana, "Plan AxB", 1);

        search(ana, "%")
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].name").value("100% done"));
        search(ana, "A_B")
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Plan A_B"));
    }

    @Test
    void aSearchWithNoMatchIsAnEmptyPageNotAnError() throws Exception {
        project(ana, "Website redesign", 1);

        mvc.perform(get("/api/v1/projects?search=nothing-like-this").with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void aSearchTextOverOneHundredCharactersIsRefused() throws Exception {
        mvc.perform(get("/api/v1/projects?search=" + "x".repeat(101)).with(as(ana)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("search"));
        mvc.perform(get("/api/v1/projects?search=" + "x".repeat(100)).with(as(ana))).andExpect(status().isOk());
    }

    @Test
    void searchResultsArePaged() throws Exception {
        project(ana, "Alpha plan", 3);
        project(ana, "Beta plan", 2);
        project(ana, "Gamma plan", 1);
        project(ana, "Unrelated", 1);

        mvc.perform(get("/api/v1/projects?search=plan&size=2").with(as(ana)))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/v1/projects?search=plan&size=2&page=1").with(as(ana)))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].name").value("Alpha plan"));
    }

    // ---- deleting (AC-11.1 to AC-11.4) ----

    @Test
    void theOwnerDeletesTheProjectAndEverythingInsideItAndNothingElse() throws Exception {
        long doomed = project(ana, "Website redesign", 2);
        member(doomed, rui, "MEMBER");
        member(doomed, sofia, "VIEWER");
        long doomedTask = task(doomed, "Doomed task", "TODO", "MEDIUM", 10);
        assign(doomedTask, rui, doomed);
        jdbc.update("INSERT INTO comments (task_id, author_id, content) VALUES (?, ?, 'A comment')", doomedTask, ana.getId());

        long kept = project(ana, "Client onboarding", 1);
        member(kept, rui, "MEMBER");
        long keptTask = task(kept, "Kept task", "TODO", "MEDIUM", 5);
        assign(keptTask, rui, kept);
        jdbc.update("INSERT INTO comments (task_id, author_id, content) VALUES (?, ?, 'Kept comment')", keptTask, rui.getId());

        deleteProject(ana, doomed).andExpect(status().isNoContent());

        assertThat(count("projects")).isEqualTo(1);
        assertThat(count("project_members")).isEqualTo(2);   // only the other project's
        assertThat(count("tasks")).isEqualTo(1);
        assertThat(count("task_assignments")).isEqualTo(1);
        assertThat(count("comments")).isEqualTo(1);
        assertThat(count("users")).isEqualTo(3);
        mvc.perform(get("/api/v1/projects/" + kept).with(as(ana))).andExpect(status().isOk());
    }

    @Test
    void aMemberAndAViewerCannotDeleteTheProject() throws Exception {
        long id = project(ana, "Website redesign", 1);
        member(id, rui, "MEMBER");
        member(id, sofia, "VIEWER");
        task(id, "A task", "TODO", "MEDIUM", 5);

        for (User user : new User[] {rui, sofia}) {
            deleteProject(user, id)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ROLE_INSUFFICIENT"));
        }

        assertThat(count("projects")).isEqualTo(1);
        assertThat(count("tasks")).isEqualTo(1);
    }

    @Test
    void aNonMemberGets404AndNothingIsDeleted() throws Exception {
        long handbook = project(sofia, "Team handbook", 1);

        deleteProject(rui, handbook).andExpect(status().isNotFound());
        deleteProject(rui, 999999).andExpect(status().isNotFound());

        assertThat(count("projects")).isEqualTo(1);
    }

    @Test
    void aDeletedProjectAndItsTasksAreGoneForEveryone() throws Exception {
        long id = project(ana, "Website redesign", 1);
        member(id, rui, "MEMBER");
        long taskId = task(id, "A task", "TODO", "MEDIUM", 5);

        deleteProject(ana, id).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/projects/" + id).with(as(ana))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/projects/" + id).with(as(rui))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/projects/" + id + "/tasks").with(as(rui))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/tasks/" + taskId).with(as(rui))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/projects").with(as(ana))).andExpect(jsonPath("$.items", hasSize(0)));
        deleteProject(ana, id).andExpect(status().isNotFound()); // deleting twice
    }

    // The text goes through .param(), so it is encoded once, as a real client would send it.
    private ResultActions search(User user, String text) throws Exception {
        return mvc.perform(get("/api/v1/projects").param("search", text).with(as(user)));
    }

    private ResultActions deleteProject(User user, long projectId) throws Exception {
        return mvc.perform(delete("/api/v1/projects/" + projectId).with(as(user)).with(csrf()));
    }
}
