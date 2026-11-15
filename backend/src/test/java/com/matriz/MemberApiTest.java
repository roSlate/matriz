package com.matriz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.matriz.identity.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Members and roles (US-03, increment 3). */
@SpringBootTest
@AutoConfigureMockMvc
class MemberApiTest extends ApiTestBase {

    long website;   // Ana owner, Rui member, Sofia viewer
    long handbook;  // Sofia owner; Ana and Rui have no access
    User tom;       // a registered user who belongs to no project

    @BeforeEach
    void projects() {
        website = project(ana, "Website redesign", 2);
        member(website, rui, "MEMBER");
        member(website, sofia, "VIEWER");
        handbook = project(sofia, "Team handbook", 1);
        tom = users.save(new User("Tom Reis", "tom@example.org", "not-a-real-hash"));
    }

    // ---- listing (AC-03.9: a Viewer sees the members) ----

    @Test
    void everyMemberCanListTheMembersOwnerFirstWithoutPasswordHashes() throws Exception {
        for (User caller : List.of(ana, rui, sofia)) {
            mvc.perform(get(members(website)).with(as(caller)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].name").value("Ana Silva"))
                    .andExpect(jsonPath("$[0].role").value("OWNER"))
                    .andExpect(jsonPath("$[1].name").value("Rui Costa"))
                    .andExpect(jsonPath("$[2].name").value("Sofia Lima"))
                    .andExpect(content().string(not(containsString("not-a-real-hash"))));
        }
    }

    @Test
    void aNonMemberGets404ForTheListAndForEveryOperation() throws Exception {
        mvc.perform(get(members(handbook)).with(as(ana))).andExpect(status().isNotFound());
        add(ana, handbook, "{\"email\":\"tom@example.org\",\"role\":\"MEMBER\"}").andExpect(status().isNotFound());
        changeRole(ana, handbook, sofia, "{\"role\":\"VIEWER\"}").andExpect(status().isNotFound());
        remove(ana, handbook, sofia).andExpect(status().isNotFound());
        assertThat(count("project_members")).isEqualTo(4);
    }

    @Test
    void aNonMemberGets404EvenWithAnInvalidBody() throws Exception {
        add(ana, handbook, "{\"email\":\"\",\"role\":\"OWNER\"}").andExpect(status().isNotFound());
        add(ana, handbook, "not json").andExpect(status().isNotFound());
        changeRole(ana, handbook, sofia, "{}").andExpect(status().isNotFound());
    }

    // ---- adding (AC-03.1, 03.3, 03.4, 03.7, 03.9) ----

    @Test
    void theOwnerAddsAnExistingUserAsMember() throws Exception {
        add(ana, website, "{\"email\":\"tom@example.org\",\"role\":\"MEMBER\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(tom.getId()))
                .andExpect(jsonPath("$.name").value("Tom Reis"))
                .andExpect(jsonPath("$.email").value("tom@example.org"))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        assertThat(roleOf(website, tom)).isEqualTo("MEMBER");
        // The new member can now see the project and write tasks, but not edit the project.
        mvc.perform(get("/api/v1/projects/" + website).with(as(tom))).andExpect(status().isOk());
        send(tom, post("/api/v1/projects/" + website + "/tasks"), "{\"title\":\"First task\"}")
                .andExpect(status().isCreated());
        send(tom, patch("/api/v1/projects/" + website), "{\"name\":\"Renamed\"}").andExpect(status().isForbidden());
    }

    @Test
    void theEmailIsTrimmedAndMatchedIgnoringLetterCase() throws Exception {
        add(ana, website, "{\"email\":\"  TOM@Example.ORG \",\"role\":\"VIEWER\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("tom@example.org"));
    }

    @Test
    void anAddedViewerCanReadButNotWrite() throws Exception {
        add(ana, website, "{\"email\":\"tom@example.org\",\"role\":\"VIEWER\"}").andExpect(status().isCreated());

        mvc.perform(get("/api/v1/projects/" + website + "/tasks").with(as(tom))).andExpect(status().isOk());
        mvc.perform(get(members(website)).with(as(tom))).andExpect(status().isOk());
        send(tom, post("/api/v1/projects/" + website + "/tasks"), "{\"title\":\"A task\"}")
                .andExpect(status().isForbidden());
        assertThat(count("tasks")).isZero();
    }

    @Test
    void anEmailThatBelongsToNoUserGives422AndAddsNothing() throws Exception {
        add(ana, website, "{\"email\":\"nobody@example.org\",\"role\":\"MEMBER\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        assertThat(count("project_members")).isEqualTo(4);
    }

    @Test
    void addingAnExistingMemberGives409AndNoDuplicate() throws Exception {
        add(ana, website, "{\"email\":\"rui@example.org\",\"role\":\"VIEWER\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_MEMBER"));
        add(ana, website, "{\"email\":\"ana@example.org\",\"role\":\"MEMBER\"}").andExpect(status().isConflict());
        assertThat(count("project_members")).isEqualTo(4);
        assertThat(roleOf(website, rui)).isEqualTo("MEMBER"); // the role was not changed by the attempt
    }

    @Test
    void onlyMemberAndViewerCanBeGiven_neverOwner() throws Exception {
        add(ana, website, "{\"email\":\"tom@example.org\",\"role\":\"OWNER\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("role"));
        assertThat(count("project_members")).isEqualTo(4);
    }

    @Test
    void invalidInputIsRejectedWith400() throws Exception {
        for (String body : List.of(
                "{\"role\":\"MEMBER\"}",
                "{\"email\":\"   \",\"role\":\"MEMBER\"}",
                "{\"email\":\"" + "x".repeat(250) + "@e.org\",\"role\":\"MEMBER\"}",
                "{\"email\":\"tom@example.org\"}",
                "{\"email\":\"tom@example.org\",\"role\":\"BOSS\"}",
                "{\"email\":\"tom@example.org\",\"role\":\"MEMBER\",\"userId\":1}",
                "not json")) {
            add(ana, website, body).andExpect(status().isBadRequest());
        }
        assertThat(count("project_members")).isEqualTo(4);
    }

    @Test
    void aMemberOrViewerCannotAdd_andInputIsCheckedBeforeTheRole() throws Exception {
        for (User caller : List.of(rui, sofia)) {
            add(caller, website, "{\"email\":\"tom@example.org\",\"role\":\"MEMBER\"}")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ROLE_INSUFFICIENT"));
            add(caller, website, "{\"email\":\"\",\"role\":\"MEMBER\"}").andExpect(status().isBadRequest());
        }
        assertThat(count("project_members")).isEqualTo(4);
    }

    // ---- changing a role (AC-03.2, 03.3, 03.6, 03.8, EC-08, EC-11) ----

    @Test
    void theOwnerChangesARole_andItAppliesFromTheNextRequest() throws Exception {
        long taskId = task(website, "Existing task", "TODO", "MEDIUM", 5);
        send(rui, patch("/api/v1/tasks/" + taskId), "{\"status\":\"IN_PROGRESS\"}").andExpect(status().isOk());

        changeRole(ana, website, rui, "{\"role\":\"VIEWER\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andExpect(jsonPath("$.name").value("Rui Costa"));

        // The same user, with no new login, is now refused writes but can still read.
        send(rui, patch("/api/v1/tasks/" + taskId), "{\"status\":\"DONE\"}").andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/tasks/" + taskId).with(as(rui))).andExpect(status().isOk());

        changeRole(ana, website, sofia, "{\"role\":\"MEMBER\"}").andExpect(status().isOk());
        send(sofia, post("/api/v1/projects/" + website + "/tasks"), "{\"title\":\"Sofia can write\"}")
                .andExpect(status().isCreated());
    }

    @Test
    void demotingAMemberToViewerRemovesTheirAssignmentsAndKeepsTheTasks() throws Exception {
        long t1 = task(website, "First", "TODO", "MEDIUM", 5);
        long t2 = task(website, "Second", "TODO", "MEDIUM", 4);
        assign(t1, rui, website);
        assign(t2, rui, website);
        assign(t1, ana, website);

        changeRole(ana, website, rui, "{\"role\":\"VIEWER\"}").andExpect(status().isOk());

        assertThat(count("task_assignments")).isEqualTo(1); // only Ana's remains
        assertThat(jdbc.queryForObject("SELECT user_id FROM task_assignments", Long.class)).isEqualTo(ana.getId());
        assertThat(count("tasks")).isEqualTo(2);
    }

    @Test
    void demotionRemovesAssignmentsOnlyInThatProject() throws Exception {
        long other = project(rui, "Client onboarding", 3);
        member(other, ana, "MEMBER");
        long inOther = task(other, "Other project task", "TODO", "MEDIUM", 5);
        assign(inOther, ana, other);
        long inWebsite = task(website, "Website task", "TODO", "MEDIUM", 5);
        assign(inWebsite, ana, website); // Ana is the Owner here, so this one is not touched either

        changeRole(ana, website, rui, "{\"role\":\"VIEWER\"}").andExpect(status().isOk());

        assertThat(count("task_assignments")).isEqualTo(2);
    }

    @Test
    void promotingAViewerToMemberKeepsEverythingAndChangingToTheSameRoleIsHarmless() throws Exception {
        long t = task(website, "A task", "TODO", "MEDIUM", 5);
        assign(t, rui, website);

        changeRole(ana, website, rui, "{\"role\":\"MEMBER\"}").andExpect(status().isOk());
        assertThat(count("task_assignments")).isEqualTo(1);

        changeRole(ana, website, sofia, "{\"role\":\"MEMBER\"}").andExpect(status().isOk());
        changeRole(ana, website, sofia, "{\"role\":\"MEMBER\"}").andExpect(status().isOk());
        assertThat(roleOf(website, sofia)).isEqualTo("MEMBER");
    }

    @Test
    void theOwnerCannotBeDemoted_andNobodyCanBecomeASecondOwner() throws Exception {
        changeRole(ana, website, ana, "{\"role\":\"MEMBER\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OWNER_PROTECTED"));
        changeRole(ana, website, rui, "{\"role\":\"OWNER\"}").andExpect(status().isBadRequest());

        assertThat(roleOf(website, ana)).isEqualTo("OWNER");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM project_members WHERE project_id = ? AND role = 'OWNER'",
                Integer.class, website)).isEqualTo(1);
    }

    @Test
    void changingTheRoleOfSomeoneWhoIsNotAMemberGives404() throws Exception {
        // Tom is a registered user but belongs to no project, so he is not reachable through this one.
        changeRole(ana, website, tom, "{\"role\":\"MEMBER\"}").andExpect(status().isNotFound());
        mvc.perform(patch(members(website) + "/abc").with(as(ana)).with(csrf())
                .contentType("application/json").content("{\"role\":\"VIEWER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aMemberOrViewerCannotChangeRoles_andInputIsCheckedBeforeTheRole() throws Exception {
        for (User caller : List.of(rui, sofia)) {
            changeRole(caller, website, sofia, "{\"role\":\"MEMBER\"}").andExpect(status().isForbidden());
            changeRole(caller, website, rui, "{\"role\":\"VIEWER\"}").andExpect(status().isForbidden());
            changeRole(caller, website, rui, "{}").andExpect(status().isBadRequest());
        }
        assertThat(roleOf(website, rui)).isEqualTo("MEMBER");
        assertThat(roleOf(website, sofia)).isEqualTo("VIEWER");
    }

    // ---- removing (AC-03.5, 03.6, EC-02) ----

    @Test
    void removingAMemberEndsTheirAccessAtOnce_andRemovesTheirAssignmentsButNotTheirWork() throws Exception {
        long t = task(website, "Rui's task", "TODO", "MEDIUM", 5);
        assign(t, rui, website);
        jdbc.update("INSERT INTO comments (task_id, author_id, content) VALUES (?, ?, 'A comment by Rui')",
                t, rui.getId());
        mvc.perform(get("/api/v1/projects/" + website).with(as(rui))).andExpect(status().isOk());

        remove(ana, website, rui).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/projects/" + website).with(as(rui))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/tasks/" + t).with(as(rui))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/projects").with(as(rui))).andExpect(jsonPath("$.totalItems").value(0));
        assertThat(count("task_assignments")).isZero();
        assertThat(count("tasks")).isEqualTo(1);
        assertThat(count("comments")).isEqualTo(1);
        assertThat(count("users")).isEqualTo(4);
    }

    @Test
    void aRemovedMemberCanBeAddedAgain() throws Exception {
        remove(ana, website, rui).andExpect(status().isNoContent());
        add(ana, website, "{\"email\":\"rui@example.org\",\"role\":\"VIEWER\"}").andExpect(status().isCreated());
        assertThat(roleOf(website, rui)).isEqualTo("VIEWER");
    }

    @Test
    void theOwnerCannotBeRemoved() throws Exception {
        remove(ana, website, ana)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OWNER_PROTECTED"));
        assertThat(roleOf(website, ana)).isEqualTo("OWNER");
    }

    @Test
    void removingSomeoneWhoIsNotAMemberGives404_andMalformedIdsGive400() throws Exception {
        remove(ana, website, tom).andExpect(status().isNotFound());
        mvc.perform(delete(members(website) + "/0").with(as(ana)).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(delete(members(website) + "/abc").with(as(ana)).with(csrf())).andExpect(status().isBadRequest());
    }

    @Test
    void aMemberOrViewerCannotRemoveAnyone() throws Exception {
        for (User caller : List.of(rui, sofia)) {
            remove(caller, website, sofia).andExpect(status().isForbidden());
            remove(caller, website, ana).andExpect(status().isForbidden());
            remove(caller, website, caller).andExpect(status().isForbidden()); // not even themselves
        }
        assertThat(count("project_members")).isEqualTo(4);
    }

    @Test
    void aRemovalInOneProjectLeavesTheSameUserInTheOthers() throws Exception {
        long other = project(rui, "Client onboarding", 3);
        member(other, ana, "MEMBER");

        remove(ana, website, rui).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/projects/" + other).with(as(ana))).andExpect(status().isOk());
        assertThat(roleOf(other, ana)).isEqualTo("MEMBER");
    }

    // ---- helpers ----

    private static String members(long projectId) {
        return "/api/v1/projects/" + projectId + "/members";
    }

    private ResultActions add(User caller, long projectId, String json) throws Exception {
        return send(caller, post(members(projectId)), json);
    }

    private ResultActions changeRole(User caller, long projectId, User target, String json) throws Exception {
        return send(caller, patch(members(projectId) + "/" + target.getId()), json);
    }

    private ResultActions remove(User caller, long projectId, User target) throws Exception {
        return mvc.perform(delete(members(projectId) + "/" + target.getId()).with(as(caller)).with(csrf()));
    }

    private String roleOf(long projectId, User user) {
        return jdbc.queryForObject("SELECT role FROM project_members WHERE project_id = ? AND user_id = ?",
                String.class, projectId, user.getId());
    }
}
