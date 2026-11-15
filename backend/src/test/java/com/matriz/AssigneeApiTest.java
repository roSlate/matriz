package com.matriz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matriz.identity.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Assigning people to tasks (US-07, increment 5). */
@SpringBootTest
@AutoConfigureMockMvc
class AssigneeApiTest extends ApiTestBase {

    long website;   // Ana owner, Rui member, Sofia viewer
    long handbook;  // Sofia owner; Ana and Rui have no access
    long taskId;    // a task of "Website redesign"
    long otherTask; // a task of "Team handbook"
    User tom;       // registered, belongs to no project

    @BeforeEach
    void fixtures() {
        website = project(ana, "Website redesign", 2);
        member(website, rui, "MEMBER");
        member(website, sofia, "VIEWER");
        handbook = project(sofia, "Team handbook", 1);
        taskId = task(website, "Write the brief", "TODO", "MEDIUM", 5);
        otherTask = task(handbook, "Outline", "TODO", "MEDIUM", 5);
        tom = users.save(new User("Tom Reis", "tom@example.org", "not-a-real-hash"));
    }

    // ---- assigning (AC-07.1, 07.4) ----

    @Test
    void anOwnerOrMemberAssignsAnOwnerOrMember_andItIsStoredAndReadBack() throws Exception {
        assign(ana, taskId, rui)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.assignees", hasSize(1)))
                .andExpect(jsonPath("$.assignees[0].name").value("Rui Costa"));
        assign(rui, taskId, ana)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignees", hasSize(2)))
                .andExpect(jsonPath("$.assignees[0].name").value("Ana Silva")); // by name

        assertThat(count("task_assignments")).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT project_id FROM task_assignments WHERE user_id = ?", Long.class,
                rui.getId())).isEqualTo(website);

        mvc.perform(get("/api/v1/tasks/" + taskId).with(as(sofia)))
                .andExpect(jsonPath("$.assignees", hasSize(2)));
        mvc.perform(get("/api/v1/projects/" + website + "/tasks").with(as(rui)))
                .andExpect(jsonPath("$.items[0].assignees", hasSize(2)));
    }

    @Test
    void aViewerAndOutsidersCannotBeAssigned_with422() throws Exception {
        for (User candidate : List.of(sofia, tom)) { // a Viewer of the project; a user outside it
            assign(ana, taskId, candidate)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("USER_NOT_ASSIGNABLE"));
        }
        send(ana, post(assignees(taskId)), "{\"userId\":99999}").andExpect(status().isUnprocessableEntity());
        assertThat(count("task_assignments")).isZero();
    }

    @Test
    void theOwnerOfAnotherProjectIsNotEligibleHere() throws Exception {
        // Sofia owns "Team handbook" but is only a Viewer of "Website redesign".
        assign(ana, taskId, sofia).andExpect(status().isUnprocessableEntity());
        // And Ana, who is not in "Team handbook" at all, cannot reach its task, let alone assign.
        assign(ana, otherTask, ana).andExpect(status().isNotFound());
    }

    @Test
    void assigningTheSameUserTwiceGives409AndNoDuplicate() throws Exception {
        assign(ana, taskId, rui).andExpect(status().isCreated());
        assign(rui, taskId, rui)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_ASSIGNED"));
        assertThat(count("task_assignments")).isEqualTo(1);
    }

    @Test
    void theSameUserCanBeAssignedToDifferentTasks() throws Exception {
        long second = task(website, "Second task", "TODO", "MEDIUM", 4);
        assign(ana, taskId, rui).andExpect(status().isCreated());
        assign(ana, second, rui).andExpect(status().isCreated());
        assertThat(count("task_assignments")).isEqualTo(2);
    }

    @Test
    void invalidInputIsRejectedWith400() throws Exception {
        for (String body : List.of("{}", "{\"userId\":null}", "{\"userId\":0}", "{\"userId\":-4}",
                "{\"userId\":\"abc\"}", "{\"userId\":1,\"role\":\"OWNER\"}", "not json")) {
            send(ana, post(assignees(taskId)), body).andExpect(status().isBadRequest());
        }
        assertThat(count("task_assignments")).isZero();
    }

    // ---- who may assign (AC-07.5) ----

    @Test
    void aViewerCannotAssignOrUnassign_andInputIsCheckedFirst() throws Exception {
        assign(ana, taskId, rui).andExpect(status().isCreated());

        assign(sofia, taskId, rui).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_INSUFFICIENT"));
        assign(sofia, taskId, ana).andExpect(status().isForbidden());
        unassign(sofia, taskId, rui).andExpect(status().isForbidden());
        send(sofia, post(assignees(taskId)), "{}").andExpect(status().isBadRequest());

        assertThat(count("task_assignments")).isEqualTo(1);
    }

    @Test
    void aNonMemberGets404ForEverything_alsoWithAnInvalidBody() throws Exception {
        assign(ana, otherTask, sofia).andExpect(status().isNotFound());
        send(ana, post(assignees(otherTask)), "{}").andExpect(status().isNotFound());
        send(ana, post(assignees(otherTask)), "not json").andExpect(status().isNotFound());
        unassign(ana, otherTask, sofia).andExpect(status().isNotFound());
        assign(ana, 999999, rui).andExpect(status().isNotFound()); // a task that does not exist
        mvc.perform(post("/api/v1/tasks/abc/assignees").with(as(ana)).with(csrf())
                .contentType("application/json").content("{\"userId\":1}")).andExpect(status().isBadRequest());
        assertThat(count("task_assignments")).isZero();
    }

    // ---- unassigning (AC-07.2) ----

    @Test
    void unassigningOnePersonLeavesTheOthers() throws Exception {
        assign(ana, taskId, ana).andExpect(status().isCreated());
        assign(ana, taskId, rui).andExpect(status().isCreated());

        unassign(rui, taskId, ana).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/tasks/" + taskId).with(as(ana)))
                .andExpect(jsonPath("$.assignees", hasSize(1)))
                .andExpect(jsonPath("$.assignees[0].name").value("Rui Costa"));
        assertThat(count("tasks")).isEqualTo(2);
    }

    @Test
    void unassigningTheLastPersonLeavesTheTaskUnassigned_andAPersonCanBeAssignedAgain() throws Exception {
        assign(ana, taskId, rui).andExpect(status().isCreated());
        unassign(ana, taskId, rui).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/tasks/" + taskId).with(as(ana))).andExpect(jsonPath("$.assignees", hasSize(0)));
        assign(ana, taskId, rui).andExpect(status().isCreated());
    }

    @Test
    void unassigningSomeoneWhoIsNotAssignedGives404_andMalformedIdsGive400() throws Exception {
        unassign(ana, taskId, rui).andExpect(status().isNotFound());
        mvc.perform(delete(assignees(taskId) + "/abc").with(as(ana)).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(delete(assignees(taskId) + "/0").with(as(ana)).with(csrf())).andExpect(status().isBadRequest());
    }

    // ---- cleanup when membership changes (AC-07.6, EC-02, EC-11), through the real API ----

    @Test
    void demotingAnAssigneeToViewerRemovesTheirAssignmentButKeepsTheTaskAndTheOthers() throws Exception {
        assign(ana, taskId, rui).andExpect(status().isCreated());
        assign(ana, taskId, ana).andExpect(status().isCreated());

        send(ana, patch("/api/v1/projects/" + website + "/members/" + rui.getId()), "{\"role\":\"VIEWER\"}")
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/tasks/" + taskId).with(as(ana)))
                .andExpect(jsonPath("$.assignees", hasSize(1)))
                .andExpect(jsonPath("$.assignees[0].name").value("Ana Silva"));
        assign(ana, taskId, rui).andExpect(status().isUnprocessableEntity()); // no longer eligible
    }

    @Test
    void removingAnAssigneeFromTheProjectRemovesTheirAssignmentButKeepsTheTask() throws Exception {
        assign(ana, taskId, rui).andExpect(status().isCreated());

        mvc.perform(delete("/api/v1/projects/" + website + "/members/" + rui.getId()).with(as(ana)).with(csrf()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/tasks/" + taskId).with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignees", hasSize(0)));
        assertThat(count("task_assignments")).isZero();
    }

    // ---- helpers ----

    private static String assignees(long taskId) {
        return "/api/v1/tasks/" + taskId + "/assignees";
    }

    private ResultActions assign(User caller, long taskId, User target) throws Exception {
        return send(caller, post(assignees(taskId)), "{\"userId\":" + target.getId() + "}");
    }

    private ResultActions unassign(User caller, long taskId, User target) throws Exception {
        return mvc.perform(delete(assignees(taskId) + "/" + target.getId()).with(as(caller)).with(csrf()));
    }
}
