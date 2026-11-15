package com.matriz;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

/**
 * Errors that no controller produces (an unknown route, a query value of the wrong type) still
 * answer with fixed texts in the standard format, and never with wording from the framework (CC-02).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ErrorResponsesTest extends ApiTestBase {

    @Test
    void anUnknownRouteGivesAFixedNotFoundMessage() throws Exception {
        for (String path : List.of("/api/v1/nothing-here", "/api/v1/projects/1/unknown", "/api/v1/tasks/1/x/y")) {
            mvc.perform(get(path).with(as(ana)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Resource not found."))
                    .andExpect(content().string(not(containsString("static resource"))))
                    .andExpect(content().string(not(containsString("Spring"))));
        }
    }

    @Test
    void aQueryValueOfTheWrongTypeGivesAFixedBadRequestMessage() throws Exception {
        for (String query : List.of("?page=abc", "?size=1.5", "?page=2x")) {
            mvc.perform(get("/api/v1/projects" + query).with(as(ana)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("A query parameter has an invalid value."))
                    .andExpect(content().string(not(containsString("Failed to convert"))))
                    .andExpect(content().string(not(containsString("java."))));
        }
    }

    @Test
    void anUnknownRouteWithoutLoginStillGives401_soRouteNamesAreNotRevealed() throws Exception {
        mvc.perform(get("/api/v1/nothing-here")).andExpect(status().isUnauthorized());
    }
}
