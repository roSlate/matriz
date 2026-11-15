package com.matriz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matriz.identity.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** The Owner's manual progress (US-10, increment 8). */
@SpringBootTest
@AutoConfigureMockMvc
class ProgressOverrideApiTest extends ApiTestBase {

    long website;   // Ana owner, Rui member, Sofia viewer; 4 tasks, 1 done: computed 25 %
    long handbook;  // Sofia owner; Ana and Rui have no access
    long doneTask;
    long openTask;

    @BeforeEach
    void fixtures() {
        website = project(ana, "Website redesign", 2);
        member(website, rui, "MEMBER");
        member(website, sofia, "VIEWER");
        handbook = project(sofia, "Team handbook", 1);
        doneTask = task(website, "Done task", "DONE", "MEDIUM", 5);
        openTask = task(website, "Open task", "TODO", "MEDIUM", 4);
        task(website, "Another open task", "IN_PROGRESS", "MEDIUM", 3);
        task(website, "A third open task", "TODO", "MEDIUM", 2);
    }

    // ---- setting (AC-10.1) ----

    @Test
    void theOwnerSetsAValue_andTheComputedValueIsShownAlongside() throws Exception {
        setOverride(ana, website, 80)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(website))
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.progress.manual").value(80))
                .andExpect(jsonPath("$.progress.computed").value(25))
                .andExpect(jsonPath("$.progress.effective").value(80))
                .andExpect(jsonPath("$.taskCounts.total").value(4));

        assertThat(jdbc.queryForObject("SELECT progress_override FROM projects WHERE id = ?", Integer.class, website))
                .isEqualTo(80);
        mvc.perform(get("/api/v1/projects/" + website).with(as(sofia)))
                .andExpect(jsonPath("$.progress.manual").value(80))
                .andExpect(jsonPath("$.progress.effective").value(80));
        mvc.perform(get("/api/v1/projects").with(as(rui)))
                .andExpect(jsonPath("$.items[0].progress.manual").value(80))
                .andExpect(jsonPath("$.items[0].progress.effective").value(80))
                .andExpect(jsonPath("$.items[0].progress.computed").value(25));
    }

    @Test
    void theBoundariesZeroAndOneHundredAreValues_andZeroIsNotNone() throws Exception {
        setOverride(ana, website, 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress.manual").value(0))
                .andExpect(jsonPath("$.progress.effective").value(0)); // not the computed 25
        assertThat(jdbc.queryForObject("SELECT progress_override FROM projects WHERE id = ?", Integer.class, website))
                .isZero();

        setOverride(ana, website, 100)
                .andExpect(jsonPath("$.progress.manual").value(100))
                .andExpect(jsonPath("$.progress.effective").value(100));
        setOverride(ana, website, 1).andExpect(jsonPath("$.progress.effective").value(1));
    }

    @Test
    void aNewValueReplacesTheOldOne() throws Exception {
        setOverride(ana, website, 80).andExpect(status().isOk());
        setOverride(ana, website, 30).andExpect(jsonPath("$.progress.manual").value(30));
        assertThat(jdbc.queryForObject("SELECT progress_override FROM projects WHERE id = ?", Integer.class, website))
                .isEqualTo(30);
    }

    // ---- invalid values (AC-10.2) ----

    @Test
    void aValueThatIsNotAWholeNumberFrom0To100IsRefusedAndChangesNothing() throws Exception {
        setOverride(ana, website, 40).andExpect(status().isOk());

        for (String body : List.of(
                "{\"percentage\":-1}", "{\"percentage\":101}", "{\"percentage\":1000}", "{\"percentage\":99999999999}",
                "{\"percentage\":80.5}", "{\"percentage\":80.0}", "{\"percentage\":\"80\"}", "{\"percentage\":\"abc\"}",
                "{\"percentage\":true}", "{\"percentage\":null}", "{\"percentage\":[80]}", "{}",
                "{\"percentage\":50,\"manual\":50}", "{\"percentage\":50,\"computed\":0}", "not json", "")) {
            int answered = send(ana, put(override(website)), body).andReturn().getResponse().getStatus();
            assertThat(answered).as("status for body '" + body + "'").isEqualTo(400);
        }
        assertThat(jdbc.queryForObject("SELECT progress_override FROM projects WHERE id = ?", Integer.class, website))
                .isEqualTo(40);
    }

    // ---- who may set it (AC-10.4) ----

    @Test
    void aMemberOrViewerCannotSetOrClear_andInputIsCheckedFirst() throws Exception {
        setOverride(ana, website, 70).andExpect(status().isOk());

        for (User caller : List.of(rui, sofia)) {
            setOverride(caller, website, 10)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ROLE_INSUFFICIENT"));
            clearOverride(caller, website).andExpect(status().isForbidden());
            send(caller, put(override(website)), "{\"percentage\":101}").andExpect(status().isBadRequest());
        }
        assertThat(jdbc.queryForObject("SELECT progress_override FROM projects WHERE id = ?", Integer.class, website))
                .isEqualTo(70);
    }

    @Test
    void aNonMemberGets404_alsoWithAnInvalidBody() throws Exception {
        setOverride(ana, handbook, 10).andExpect(status().isNotFound());
        send(ana, put(override(handbook)), "{\"percentage\":500}").andExpect(status().isNotFound());
        send(ana, put(override(handbook)), "not json").andExpect(status().isNotFound());
        clearOverride(ana, handbook).andExpect(status().isNotFound());
        setOverride(ana, 999999, 10).andExpect(status().isNotFound()); // no such project
        mvc.perform(put("/api/v1/projects/abc/progress-override").with(as(ana)).with(csrf())
                .contentType("application/json").content("{\"percentage\":10}")).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT progress_override FROM projects WHERE id = ?", Integer.class, handbook))
                .isNull();
    }

    // ---- clearing (AC-10.3) ----

    @Test
    void clearingReturnsToTheComputedValue_andClearingNothingIsHarmless() throws Exception {
        setOverride(ana, website, 80).andExpect(status().isOk());

        clearOverride(ana, website)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress.manual").value(nullValue()))
                .andExpect(jsonPath("$.progress.computed").value(25))
                .andExpect(jsonPath("$.progress.effective").value(25));
        assertThat(jdbc.queryForObject("SELECT progress_override FROM projects WHERE id = ?", Integer.class, website))
                .isNull();

        clearOverride(ana, website).andExpect(status().isOk()).andExpect(jsonPath("$.progress.effective").value(25));
    }

    // ---- the manual value and the tasks (AC-10.5) ----

    @Test
    void whenTasksChangeTheManualValueStaysAndTheComputedValueFollows() throws Exception {
        setOverride(ana, website, 80).andExpect(status().isOk());

        send(rui, patch("/api/v1/tasks/" + openTask), "{\"status\":\"DONE\"}").andExpect(status().isOk());
        mvc.perform(get("/api/v1/projects/" + website).with(as(ana)))
                .andExpect(jsonPath("$.progress.manual").value(80))
                .andExpect(jsonPath("$.progress.computed").value(50)) // 2 of 4
                .andExpect(jsonPath("$.progress.effective").value(80));

        mvc.perform(delete("/api/v1/tasks/" + doneTask).with(as(ana)).with(csrf())).andExpect(status().isNoContent());
        send(ana, post("/api/v1/projects/" + website + "/tasks"), "{\"title\":\"New task\"}").andExpect(status().isCreated());
        mvc.perform(get("/api/v1/projects/" + website).with(as(ana)))
                .andExpect(jsonPath("$.progress.manual").value(80))
                .andExpect(jsonPath("$.progress.computed").value(25)); // 1 of 4
    }

    @Test
    void aProjectWithoutTasksCanHaveAManualValue() throws Exception {
        long empty = project(ana, "Empty project", 1);
        setOverride(ana, empty, 60)
                .andExpect(jsonPath("$.progress.computed").value(0))
                .andExpect(jsonPath("$.progress.manual").value(60))
                .andExpect(jsonPath("$.progress.effective").value(60));
    }

    @Test
    void theOverrideBelongsToItsOwnProjectOnly() throws Exception {
        long other = project(ana, "Another project", 1);
        setOverride(ana, website, 80).andExpect(status().isOk());
        mvc.perform(get("/api/v1/projects/" + other).with(as(ana)))
                .andExpect(jsonPath("$.progress.manual").value(nullValue()));
    }

    // ---- helpers ----

    private static String override(long projectId) {
        return "/api/v1/projects/" + projectId + "/progress-override";
    }

    private ResultActions setOverride(User caller, long projectId, int percentage) throws Exception {
        return send(caller, put(override(projectId)), "{\"percentage\":" + percentage + "}");
    }

    private ResultActions clearOverride(User caller, long projectId) throws Exception {
        return mvc.perform(delete(override(projectId)).with(as(caller)).with(csrf()));
    }
}
