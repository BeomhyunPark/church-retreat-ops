package com.gmc.retreat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.admin.service.SystemAdminBootstrapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GmcRetreatApplicationTests {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("church_retreat_ops_test")
            .withUsername("retreat_app")
            .withPassword("retreat_app_password");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SystemAdminBootstrapper systemAdminBootstrapper;

    @Test
    void contextLoadsAndFlywayMigrationRuns() {
        Integer baselineRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_baseline WHERE id = 1",
                Integer.class
        );

        assertThat(baselineRows).isEqualTo(1);
    }

    @Test
    void flywayMigrationCreatesAdminUsersTable() {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'admin_users'
                        """,
                Integer.class
        );

        assertThat(tableCount).isEqualTo(1);
    }

    @Test
    void bootstrapSystemAdminIsCreatedWhenMissingAndIsIdempotent() throws Exception {
        Integer adminCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_users WHERE email = 'admin@gmc.local'",
                Integer.class
        );
        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM admin_users WHERE email = 'admin@gmc.local'",
                String.class
        );

        assertThat(adminCount).isEqualTo(1);
        assertThat(passwordHash).isNotEqualTo("admin1234!");
        assertThat(passwordEncoder.matches("admin1234!", passwordHash)).isTrue();

        systemAdminBootstrapper.run(null);

        Integer adminCountAfterSecondRun = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_users WHERE email = 'admin@gmc.local'",
                Integer.class
        );
        assertThat(adminCountAfterSecondRun).isEqualTo(1);
    }

    @Test
    void loginSucceedsWithValidBootstrapAdminCredentials() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@gmc.local",
                                  "password": "admin1234!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.admin.email").value("admin@gmc.local"))
                .andExpect(jsonPath("$.data.admin.role").value("SYSTEM_ADMIN"));
    }

    @Test
    void loginFailsWithInvalidPassword() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@gmc.local",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_ADMIN_CREDENTIALS"))
                .andExpect(jsonPath("$.error.message").value("Invalid email or password."));
    }

    @Test
    void loginWithMissingRequestBodyReturnsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("Invalid request."));
    }

    @Test
    void loginWithInvalidJsonReturnsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("Invalid request."));
    }

    @Test
    void currentAdminProfileRequiresJwt() throws Exception {
        mockMvc.perform(get("/api/admin/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void currentAdminProfileReturnsAdminWhenJwtIsValid() throws Exception {
        String accessToken = loginAndGetAccessToken();

        mockMvc.perform(get("/api/admin/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("admin@gmc.local"))
                .andExpect(jsonPath("$.data.name").value("System Admin"))
                .andExpect(jsonPath("$.data.role").value("SYSTEM_ADMIN"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void currentAdminProfileRejectsJwtWithInvalidSubjectClaim() throws Exception {
        String accessToken = signedToken(Map.of(
                "sub", "not-a-number",
                "email", "admin@gmc.local",
                "name", "System Admin",
                "role", "SYSTEM_ADMIN",
                "iat", Instant.now().getEpochSecond(),
                "exp", Instant.now().plusSeconds(3600).getEpochSecond()
        ));

        mockMvc.perform(get("/api/admin/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void currentAdminProfileRejectsJwtWithUnknownRoleClaim() throws Exception {
        String accessToken = signedToken(Map.of(
                "sub", "1",
                "email", "admin@gmc.local",
                "name", "System Admin",
                "role", "OWNER",
                "iat", Instant.now().getEpochSecond(),
                "exp", Instant.now().plusSeconds(3600).getEpochSecond()
        ));

        mockMvc.perform(get("/api/admin/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void currentAdminProfileRejectsJwtWithMissingRequiredClaim() throws Exception {
        String accessToken = signedToken(Map.of(
                "sub", "1",
                "email", "admin@gmc.local",
                "role", "SYSTEM_ADMIN",
                "iat", Instant.now().getEpochSecond(),
                "exp", Instant.now().plusSeconds(3600).getEpochSecond()
        ));

        mockMvc.perform(get("/api/admin/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Nested
    class RoleHierarchyTests {

        @Test
        void pastorHasHigherAuthorityThanChair() {
            assertThat(AdminRole.CHAIR.hasAuthorityAtLeast(AdminRole.STAFF)).isTrue();
            assertThat(AdminRole.PASTOR.hasAuthorityAtLeast(AdminRole.CHAIR)).isTrue();
            assertThat(AdminRole.SYSTEM_ADMIN.hasAuthorityAtLeast(AdminRole.PASTOR)).isTrue();
            assertThat(AdminRole.CHAIR.hasAuthorityAtLeast(AdminRole.PASTOR)).isFalse();
        }
    }

    private String loginAndGetAccessToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@gmc.local",
                                  "password": "admin1234!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("accessToken").asText();
    }

    private String signedToken(Map<String, Object> payload) throws Exception {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        String headerPart = encodeJson(header);
        String payloadPart = encodeJson(payload);
        String signingInput = headerPart + "." + payloadPart;
        return signingInput + "." + sign(signingInput);
    }

    private String encodeJson(Map<String, Object> value) throws Exception {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(
                "local-dev-secret-change-me-local-dev-secret-change-me".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(secretKey);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
