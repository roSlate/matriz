package com.matriz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matriz.identity.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Comments on tasks (US-08, increment 7). */
@SpringBootTest
@AutoConfigureMockMvc
class CommentApiTest extends ApiTestBase {

    long website;   // Ana owner, Rui member, Sofia viewer
    long handbook;  // Sofia owner; Ana and Rui have no access
    long taskId;    // a task of "Website redesign"
    long otherTask; // a task of "Team handbook"

    @BeforeEach
    void fixtures() {
        website = project(ana, "Website redesign", 2);
        member(website, rui, "MEMBER");
        member(website, sofia, "VIEWER");
        handbook = project(sofia, "Team handbook", 1);
        taskId = task(website, "Write the brief", "TODO", "MEDIUM", 5);
        otherTask = task(handbook, "Outline", "TODO", "MEDIUM", 5);
    }

    // ---- adding (AC-08.1, 08.3) ----

    @Test
    void aCommentIsStoredTrimmedWithTheSessionUserAndTheServerTime_andReadBack() throws Exception {
        comment(rui, taskId, "{\"content\":\"  Waiting for the client's feedback.  \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.author.id").value(rui.getId()))
                .andExpect(jsonPath("$.author.name").value("Rui Costa"))
                .andExpect(jsonPath("$.content").value("Waiting for the client's feedback."))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertThat(jdbc.queryForObject("SELECT content || '/' || author_id FROM comments", String.class))
                .isEqualTo("Waiting for the client's feedback./" + rui.getId());
        assertThat(jdbc.queryForObject("SELECT now() - created_at < interval '1 minute' FROM comments",
                Boolean.class)).isTrue();

        mvc.perform(get(comments(taskId)).with(as(sofia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].content").value("Waiting for the client's feedback."));
    }

    @Test
    void lineBreaksInsideTheTextAreKept() throws Exception {
        comment(ana, taskId, "{\"content\":\"First line\\nSecond line\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("First line\nSecond line"));
    }

    @Test
    void theTextIsCheckedAfterTrimming_atEveryBoundary() throws Exception {
        for (String invalid : List.of("", "   ", "\\n \\t", "x".repeat(2001), " " + "x".repeat(2001))) {
            comment(ana, taskId, "{\"content\":\"" + invalid + "\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("content"));
        }
        assertThat(count("comments")).isZero();

        comment(ana, taskId, "{\"content\":\"x\"}").andExpect(status().isCreated());                      // 1
        comment(ana, taskId, "{\"content\":\"" + "x".repeat(2000) + "\"}").andExpect(status().isCreated()); // 2000
        comment(ana, taskId, "{\"content\":\"  " + "x".repeat(2000) + "  \"}").andExpect(status().isCreated());
        assertThat(count("comments")).isEqualTo(3);
    }

    @Test
    void aRequestWithAnythingBesidesTheTextIsRefused_soTheClientCannotChooseTheAuthorOrTheDate() throws Exception {
        for (String body : List.of(
                "{\"content\":\"Hello\",\"authorId\":2}",
                "{\"content\":\"Hello\",\"author\":{\"id\":2}}",
                "{\"content\":\"Hello\",\"createdAt\":\"2020-01-01T00:00:00Z\"}",
                "{\"content\":\"Hello\",\"taskId\":999}",
                "{}", "{\"content\":null}", "{\"content\":42}", "not json")) {
            int answered = comment(ana, taskId, body).andReturn().getResponse().getStatus();
            assertThat(answered).as("status for body " + body).isEqualTo(400);
        }
        assertThat(count("comments")).isZero();
    }

    // ---- who may comment (AC-08.6) ----

    @Test
    void aViewerCanReadButNotComment_andInputIsCheckedFirst() throws Exception {
        comment(ana, taskId, "{\"content\":\"A comment\"}").andExpect(status().isCreated());

        comment(sofia, taskId, "{\"content\":\"Not allowed\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_INSUFFICIENT"));
        comment(sofia, taskId, "{\"content\":\"  \"}").andExpect(status().isBadRequest());
        mvc.perform(get(comments(taskId)).with(as(sofia))).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1));
        assertThat(count("comments")).isEqualTo(1);
    }

    @Test
    void aNonMemberGets404ForReadingAndWriting_alsoWithAnInvalidBody() throws Exception {
        comment(sofia, otherTask, "{\"content\":\"Mine\"}").andExpect(status().isCreated());

        mvc.perform(get(comments(otherTask)).with(as(ana))).andExpect(status().isNotFound());
        comment(ana, otherTask, "{\"content\":\"Let me in\"}").andExpect(status().isNotFound());
        comment(ana, otherTask, "{\"content\":\"\"}").andExpect(status().isNotFound());
        comment(ana, otherTask, "not json").andExpect(status().isNotFound());
        mvc.perform(get(comments(999999)).with(as(ana))).andExpect(status().isNotFound()); // no such task
        mvc.perform(get("/api/v1/tasks/abc/comments").with(as(ana))).andExpect(status().isBadRequest());
        assertThat(count("comments")).isEqualTo(1);
    }

    @Test
    void aCommentIsOnlyListedUnderItsOwnTask() throws Exception {
        long second = task(website, "Second task", "TODO", "MEDIUM", 4);
        comment(ana, taskId, "{\"content\":\"On the first\"}").andExpect(status().isCreated());
        comment(ana, second, "{\"content\":\"On the second\"}").andExpect(status().isCreated());

        mvc.perform(get(comments(taskId)).with(as(ana)))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].content").value("On the first"));
    }

    // ---- listing (AC-08.4) ----

    @Test
    void commentsAreListedOldestFirst_twentyPerPageByDefault_withTheTotals() throws Exception {
        for (int i = 1; i <= 25; i++) {
            insertComment(taskId, rui, "Comment " + i, 100 - i); // Comment 1 is the oldest
        }

        mvc.perform(get(comments(taskId)).with(as(ana)))
                .andExpect(jsonPath("$.items", hasSize(20)))
                .andExpect(jsonPath("$.items[0].content").value("Comment 1"))
                .andExpect(jsonPath("$.items[19].content").value("Comment 20"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(25))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get(comments(taskId) + "?page=1").with(as(ana)))
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.items[0].content").value("Comment 21"))
                .andExpect(jsonPath("$.items[4].content").value("Comment 25"));
    }

    @Test
    void aNewCommentComesLast() throws Exception {
        insertComment(taskId, rui, "Older", 10);
        comment(ana, taskId, "{\"content\":\"Newest\"}").andExpect(status().isCreated());

        mvc.perform(get(comments(taskId)).with(as(ana)))
                .andExpect(jsonPath("$.items[0].content").value("Older"))
                .andExpect(jsonPath("$.items[1].content").value("Newest"));
    }

    @Test
    void aTaskWithoutCommentsGivesAnEmptyPage_andPagingLimitsAreChecked() throws Exception {
        mvc.perform(get(comments(taskId)).with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalItems").value(0));
        for (String query : List.of("?size=0", "?size=101", "?page=-1")) {
            mvc.perform(get(comments(taskId) + query).with(as(ana))).andExpect(status().isBadRequest());
        }
    }

    // ---- author privacy and removal (AC-08.5, CC-01) ----

    @Test
    void theAuthorIsShownByNameOnly_neverWithAnEmail() throws Exception {
        comment(rui, taskId, "{\"content\":\"Hello\"}")
                .andExpect(content().string(not(containsString("rui@example.org"))))
                .andExpect(content().string(not(containsString("email"))))
                .andExpect(content().string(not(containsString("not-a-real-hash"))));
        mvc.perform(get(comments(taskId)).with(as(sofia)))
                .andExpect(content().string(not(containsString("rui@example.org"))))
                .andExpect(content().string(not(containsString("email"))))
                .andExpect(jsonPath("$.items[0].author.name").value("Rui Costa"));
    }

    @Test
    void aCommentSurvivesItsAuthorLeavingTheProject_andBeingDemoted() throws Exception {
        comment(rui, taskId, "{\"content\":\"Written by Rui\"}").andExpect(status().isCreated());

        send(ana, patch("/api/v1/projects/" + website + "/members/" + rui.getId()), "{\"role\":\"VIEWER\"}")
                .andExpect(status().isOk());
        mvc.perform(get(comments(taskId)).with(as(ana)))
                .andExpect(jsonPath("$.items[0].author.name").value("Rui Costa"));

        mvc.perform(delete("/api/v1/projects/" + website + "/members/" + rui.getId()).with(as(ana)).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get(comments(taskId)).with(as(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].content").value("Written by Rui"))
                .andExpect(jsonPath("$.items[0].author.name").value("Rui Costa"))
                .andExpect(content().string(not(containsString("rui@example.org"))));
        mvc.perform(get(comments(taskId)).with(as(rui))).andExpect(status().isNotFound()); // no access any more
    }

    @Test
    void aMemberWhoBecomesAViewerCanNoLongerComment_fromTheNextRequest() throws Exception {
        comment(rui, taskId, "{\"content\":\"Before\"}").andExpect(status().isCreated());
        send(ana, patch("/api/v1/projects/" + website + "/members/" + rui.getId()), "{\"role\":\"VIEWER\"}")
                .andExpect(status().isOk());
        comment(rui, taskId, "{\"content\":\"After\"}").andExpect(status().isForbidden());
        assertThat(count("comments")).isEqualTo(1);
    }

    // ---- deletion ----

    @Test
    void deletingTheTaskDeletesItsComments_andOnlyThose() throws Exception {
        long second = task(website, "Second task", "TODO", "MEDIUM", 4);
        comment(ana, taskId, "{\"content\":\"Goes with the task\"}").andExpect(status().isCreated());
        comment(ana, second, "{\"content\":\"Stays\"}").andExpect(status().isCreated());

        mvc.perform(delete("/api/v1/tasks/" + taskId).with(as(ana)).with(csrf())).andExpect(status().isNoContent());

        assertThat(count("comments")).isEqualTo(1);
        mvc.perform(get(comments(second)).with(as(ana))).andExpect(jsonPath("$.items[0].content").value("Stays"));
    }

    // ---- helpers ----

    private static String comments(long taskId) {
        return "/api/v1/tasks/" + taskId + "/comments";
    }

    private ResultActions comment(User caller, long taskId, String json) throws Exception {
        return send(caller, post(comments(taskId)), json);
    }

    private void insertComment(long taskId, User author, String text, int minutesAgo) {
        jdbc.update("INSERT INTO comments (task_id, author_id, content, created_at) "
                + "VALUES (?, ?, ?, now() - ? * interval '1 minute')", taskId, author.getId(), text, minutesAgo);
    }
}
