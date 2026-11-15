package com.matriz;

import static org.assertj.core.api.Assertions.assertThat;

import com.matriz.identity.User;
import com.matriz.identity.UserRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

// MockMvc does not show the Set-Cookie headers that Spring Security writes, so the cookie
// rules are checked against the real embedded server, over real HTTP.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CookieAndCsrfFlowTest {

    private static final String EMAIL = "ana@example.org";
    private static final String PASSWORD = "correct-password-1";
    private static final String LOGIN_BODY = "{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @LocalServerPort
    int port;

    @Autowired
    UserRepository users;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JdbcTemplate jdbc;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void oneKnownUser() {
        jdbc.execute("TRUNCATE users RESTART IDENTITY CASCADE");
        users.save(new User("Ana Silva", EMAIL, encoder.encode(PASSWORD)));
    }

    @Test
    void anAnonymousRequestGetsACsrfCookieButNoSession() throws Exception {
        HttpResponse<String> response = get("/api/v1/users/me", null);

        assertThat(response.statusCode()).isEqualTo(401);
        List<String> cookies = response.headers().allValues("Set-Cookie");
        assertThat(cookies).anyMatch(c -> c.startsWith("XSRF-TOKEN=")
                && c.contains("SameSite=Lax") && !c.contains("HttpOnly"));
        assertThat(cookies).noneMatch(c -> c.startsWith("JSESSIONID"));
    }

    @Test
    void loginWithTheCsrfTokenStartsAnHttpOnlySession() throws Exception {
        String token = anonymousCsrfToken();

        HttpResponse<String> login = postLogin(token, token);

        assertThat(login.statusCode()).isEqualTo(200);
        List<String> cookies = login.headers().allValues("Set-Cookie");
        assertThat(cookies).anyMatch(c -> c.startsWith("JSESSIONID=")
                && c.contains("HttpOnly") && c.contains("SameSite=Lax"));
        assertThat(login.body()).doesNotContain("password").doesNotContain("$2");

        String session = cookieValue(cookies, "JSESSIONID");
        HttpResponse<String> me = get("/api/v1/users/me", "JSESSIONID=" + session);
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains(EMAIL);
    }

    @Test
    void aCsrfHeaderThatDoesNotMatchTheCookieIsRefused() throws Exception {
        String token = anonymousCsrfToken();

        HttpResponse<String> login = postLogin(token, "not-the-token");

        assertThat(login.statusCode()).isEqualTo(403);
        assertThat(login.body()).contains("CSRF_TOKEN_INVALID");
    }

    @Test
    void theOldSessionStopsWorkingAfterLogout() throws Exception {
        String token = anonymousCsrfToken();
        String session = cookieValue(postLogin(token, token).headers().allValues("Set-Cookie"), "JSESSIONID");
        String cookieHeader = "JSESSIONID=" + session + "; XSRF-TOKEN=" + token;

        HttpResponse<String> logout = send(HttpRequest.newBuilder(uri("/api/v1/auth/logout"))
                .header("Cookie", cookieHeader)
                .header("X-XSRF-TOKEN", token)
                .POST(BodyPublishers.noBody()).build());

        assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(get("/api/v1/users/me", "JSESSIONID=" + session).statusCode()).isEqualTo(401);
    }

    private String anonymousCsrfToken() throws Exception {
        return cookieValue(get("/api/v1/users/me", null).headers().allValues("Set-Cookie"), "XSRF-TOKEN");
    }

    private HttpResponse<String> postLogin(String cookieToken, String headerToken) throws Exception {
        return send(HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .header("Cookie", "XSRF-TOKEN=" + cookieToken)
                .header("X-XSRF-TOKEN", headerToken)
                .POST(BodyPublishers.ofString(LOGIN_BODY)).build());
    }

    private HttpResponse<String> get(String path, String cookieHeader) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
        if (cookieHeader != null) {
            request.header("Cookie", cookieHeader);
        }
        return send(request.build());
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return client.send(request, BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static String cookieValue(List<String> setCookieHeaders, String name) {
        return setCookieHeaders.stream()
                .filter(header -> header.startsWith(name + "="))
                .map(header -> header.substring(name.length() + 1).split(";", 2)[0])
                .findFirst()
                .orElseThrow(() -> new AssertionError("No Set-Cookie header for " + name));
    }
}
