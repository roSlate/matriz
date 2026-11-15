package com.matriz;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matriz.identity.User;
import com.matriz.identity.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationTest extends IntegrationTestBase {

    private static final String EMAIL = "ana@example.org";
    private static final String PASSWORD = "correct-password-1";

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void oneKnownUser() {
        jdbc.execute("TRUNCATE users RESTART IDENTITY CASCADE");
        users.save(new User("Ana Silva", EMAIL, encoder.encode(PASSWORD)));
    }

    @Test
    void validCredentialsStartASessionAndReturnTheUserWithoutAnyPassword() throws Exception {
        MvcResult result = login(EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Silva"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("$2"))))
                .andReturn();

        mvc.perform(get("/api/v1/users/me").session(sessionOf(result)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void emailIsMatchedIgnoringLetterCase() throws Exception {
        login("ANA@Example.ORG", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void aWrongPasswordAndAnUnknownEmailGetTheSameAnswer() throws Exception {
        login(EMAIL, "wrong-password-1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));
        login("nobody@example.org", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));
    }

    @Test
    void emptyOrBlankEmailAndPasswordAreRefusedBeforeTheCredentialsAreChecked() throws Exception {
        mvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"   \",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("email", "password")));
    }

    @Test
    void aFieldThatIsNotPartOfTheContractIsRefused() throws Exception {
        mvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void withoutASessionTheAnswerIs401InTheProblemFormat() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Authentication is required."));
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        MvcResult result = login(EMAIL, PASSWORD).andExpect(status().isOk()).andReturn();
        MockHttpSession session = sessionOf(result);

        mvc.perform(post("/api/v1/auth/logout").with(csrf()).session(session))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/users/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithoutASessionIs401() throws Exception {
        mvc.perform(post("/api/v1/auth/logout").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changingDataWithoutACsrfTokenIsRefusedWithItsOwnCode() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void theHealthCheckIsOpenButOtherRoutesAreNot() throws Exception {
        mvc.perform(get("/api/v1/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/projects")).andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private static MockHttpSession sessionOf(MvcResult result) {
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
