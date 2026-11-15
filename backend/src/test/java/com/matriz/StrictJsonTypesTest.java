package com.matriz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

/**
 * A value of the wrong JSON type is refused, not converted: the contract says {@code string} and
 * {@code integer}, so a number or a boolean sent as text, or a text sent as a number, gives
 * {@code 400} and saves nothing (NFR-04, S-05).
 */
@SpringBootTest
@AutoConfigureMockMvc
class StrictJsonTypesTest extends ApiTestBase {

    long website;
    long taskId;

    @BeforeEach
    void fixtures() {
        website = project(ana, "Website redesign", 2);
        member(website, rui, "MEMBER");
        taskId = task(website, "Write the brief", "TODO", "MEDIUM", 5);
    }

    @Test
    void aNumberOrABooleanIsNotAcceptedWhereTheContractSaysString() throws Exception {
        for (String wrong : List.of("42", "true", "1.5")) {
            send(ana, post("/api/v1/projects"), "{\"name\":" + wrong + "}").andExpect(status().isBadRequest());
            send(ana, patch("/api/v1/projects/" + website), "{\"name\":" + wrong + "}").andExpect(status().isBadRequest());
            send(ana, post("/api/v1/projects/" + website + "/tasks"), "{\"title\":" + wrong + "}")
                    .andExpect(status().isBadRequest());
            send(ana, patch("/api/v1/tasks/" + taskId), "{\"title\":" + wrong + "}").andExpect(status().isBadRequest());
            send(ana, post("/api/v1/tasks/" + taskId + "/comments"), "{\"content\":" + wrong + "}")
                    .andExpect(status().isBadRequest());
            send(ana, post("/api/v1/projects/" + website + "/members"),
                    "{\"email\":" + wrong + ",\"role\":\"VIEWER\"}").andExpect(status().isBadRequest());
        }
        assertThat(count("projects")).isEqualTo(1);
        assertThat(count("tasks")).isEqualTo(1);
        assertThat(count("comments")).isZero();
        assertThat(count("project_members")).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT name FROM projects", String.class)).isEqualTo("Website redesign");
        assertThat(jdbc.queryForObject("SELECT title FROM tasks", String.class)).isEqualTo("Write the brief");
    }

    @Test
    void aTextIsNotAcceptedWhereTheContractSaysInteger() throws Exception {
        send(ana, post("/api/v1/tasks/" + taskId + "/assignees"), "{\"userId\":\"" + rui.getId() + "\"}")
                .andExpect(status().isBadRequest());
        assertThat(count("task_assignments")).isZero();
    }

    @Test
    void aCorrectlyTypedRequestStillWorks() throws Exception {
        send(ana, post("/api/v1/projects"), "{\"name\":\"A valid name\"}").andExpect(status().isCreated());
        send(ana, post("/api/v1/tasks/" + taskId + "/assignees"), "{\"userId\":" + rui.getId() + "}")
                .andExpect(status().isCreated());
    }
}
