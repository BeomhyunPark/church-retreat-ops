package com.gmc.retreat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.admin.service.SystemAdminBootstrapper;
import com.gmc.retreat.registration.service.RegistrationProperties;
import com.gmc.retreat.registration.service.RegistrationService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
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

    @Autowired
    private RegistrationService registrationService;

    @BeforeEach
    void cleanRegistrationData() {
        jdbcTemplate.update("DELETE FROM registration_privacy_access_logs");
        jdbcTemplate.update("DELETE FROM registration_histories");
        jdbcTemplate.update("DELETE FROM registrations");
    }

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

    @Test
    void registrationCreationSucceedsAndReturnsLookupKeyOnce() throws Exception {
        MvcResult result = createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true);

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        String lookupKey = response.path("data").path("lookupKey").asText();

        assertThat(response.path("success").asBoolean()).isTrue();
        assertThat(response.path("data").path("resultType").asText()).isEqualTo("CREATED");
        assertThat(response.path("data").path("registration").path("name").asText()).isEqualTo("Grace Kim");
        assertThat(lookupKey).isNotBlank();
        assertThat(countOccurrences(responseBody, lookupKey)).isEqualTo(1);
    }

    @Test
    void databaseStoresBcryptLookupKeyHashInsteadOfPlaintextLookupKey() throws Exception {
        JsonNode response = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        String lookupKey = response.path("data").path("lookupKey").asText();

        String lookupKeyHash = jdbcTemplate.queryForObject(
                "SELECT lookup_key_hash FROM registrations WHERE name = 'Grace Kim'",
                String.class
        );

        assertThat(lookupKeyHash).isNotEqualTo(lookupKey);
        assertThat(lookupKeyHash).startsWith("$2");
        assertThat(passwordEncoder.matches(lookupKey, lookupKeyHash)).isTrue();
    }

    @Test
    void duplicateRegistrationOverwritesExistingActiveRowAndInsertsOverwrittenHistory() throws Exception {
        JsonNode first = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long registrationId = first.path("data").path("registration").path("id").asLong();

        MvcResult duplicateResult = createRegistration("Grace Kim", "01012345678", "College", true);
        JsonNode duplicate = objectMapper.readTree(duplicateResult.getResponse().getContentAsString());

        Integer activeCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM registrations
                        WHERE normalized_name = 'Grace Kim'
                          AND phone_number = '01012345678'
                          AND status = 'REGISTERED'
                        """,
                Integer.class
        );
        Integer overwrittenHistoryCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM registration_histories
                        WHERE registration_id = ?
                          AND change_type = 'OVERWRITTEN'
                        """,
                Integer.class,
                registrationId
        );

        assertThat(duplicate.path("data").path("resultType").asText()).isEqualTo("OVERWRITTEN");
        assertThat(duplicate.path("data").path("registration").path("id").asLong()).isEqualTo(registrationId);
        assertThat(duplicate.path("data").path("registration").path("churchCellDepartment").asText())
                .isEqualTo("College");
        assertThat(activeCount).isEqualTo(1);
        assertThat(overwrittenHistoryCount).isEqualTo(1);
    }

    @Test
    void selfLookupSucceedsWithNamePhoneLastFourAndLookupKey() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );

        mockMvc.perform(post("/api/registrations/self/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Grace Kim",
                                "phoneLastFour", "5678",
                                "lookupKey", created.path("data").path("lookupKey").asText()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Grace Kim"))
                .andExpect(jsonPath("$.data.phoneNumber").value("010****5678"));
    }

    @Test
    void selfLookupFailsWithWrongLookupKey() throws Exception {
        createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true);

        mockMvc.perform(post("/api/registrations/self/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Grace Kim",
                                "phoneLastFour", "5678",
                                "lookupKey", "wrong-key"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("REGISTRATION_LOOKUP_FAILED"));
    }

    @Test
    void selfUpdateSucceedsWhenSelfEditEnabled() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );

        mockMvc.perform(put("/api/registrations/self")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selfUpdateRequest(created.path("data").path("lookupKey").asText(), "010-9999-0000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phoneNumber").value("010****0000"))
                .andExpect(jsonPath("$.data.churchCellDepartment").value("Updated Cell"));

        Integer updatedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registration_histories WHERE change_type = 'SELF_UPDATED'",
                Integer.class
        );
        assertThat(updatedCount).isEqualTo(1);
    }

    @Test
    void selfUpdateFailsWhenSelfEditDisabled() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        ReflectionTestUtils.setField(registrationService, "registrationProperties", new RegistrationProperties(false));

        try {
            mockMvc.perform(put("/api/registrations/self")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(selfUpdateRequest(created.path("data").path("lookupKey").asText(), "010-9999-0000")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("REGISTRATION_EDIT_CLOSED"));
        } finally {
            ReflectionTestUtils.setField(registrationService, "registrationProperties", new RegistrationProperties(true));
        }
    }

    @Test
    void registrationCreationFailsWhenPrivacyConsentIsFalse() throws Exception {
        mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest("Grace Kim", "010-1234-5678", "Young Adults", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void adminRegistrationListAndDetailRequireJwt() throws Exception {
        mockMvc.perform(get("/api/admin/registrations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/admin/registrations/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void registrationResponsesNeverExposeLookupKeyHash() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        String lookupKey = created.path("data").path("lookupKey").asText();
        Long registrationId = created.path("data").path("registration").path("id").asLong();
        String accessToken = loginAndGetAccessToken();

        MvcResult lookupResult = mockMvc.perform(post("/api/registrations/self/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Grace Kim",
                                "phoneLastFour", "5678",
                                "lookupKey", lookupKey
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult updateResult = mockMvc.perform(put("/api/registrations/self")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selfUpdateRequest(lookupKey, "010-9999-0000")))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult adminListResult = mockMvc.perform(get("/api/admin/registrations")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult adminDetailResult = mockMvc.perform(get("/api/admin/registrations/" + registrationId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(created.toString()).doesNotContain("lookupKeyHash", "lookup_key_hash");
        assertThat(lookupResult.getResponse().getContentAsString()).doesNotContain("lookupKeyHash", "lookup_key_hash");
        assertThat(updateResult.getResponse().getContentAsString()).doesNotContain("lookupKeyHash", "lookup_key_hash");
        assertThat(adminListResult.getResponse().getContentAsString()).doesNotContain("lookupKeyHash", "lookup_key_hash");
        assertThat(adminDetailResult.getResponse().getContentAsString()).doesNotContain("lookupKeyHash", "lookup_key_hash");
    }

    @Test
    void staffCanViewParticipantListAndDetail() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long registrationId = created.path("data").path("registration").path("id").asLong();
        String staffToken = accessTokenForRole(AdminRole.STAFF);

        mockMvc.perform(get("/api/admin/registrations")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].phoneNumber").value("010****5678"));

        mockMvc.perform(get("/api/admin/registrations/" + registrationId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phoneNumber").value("01012345678"));
    }

    @Test
    void chairCanUpdateFeeStatusMemoAndCareTags() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long registrationId = created.path("data").path("registration").path("id").asLong();
        String chairToken = accessTokenForRole(AdminRole.CHAIR);

        mockMvc.perform(patch("/api/admin/registrations/" + registrationId + "/fee-paid")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("feePaid", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feePaid").value(true));

        mockMvc.perform(patch("/api/admin/registrations/" + registrationId + "/status")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CANCELLED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(patch("/api/admin/registrations/" + registrationId + "/management")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "adminMemo", "Needs first-time attendee follow-up.",
                                "newcomer", true,
                                "careTarget", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminMemo").value("Needs first-time attendee follow-up."))
                .andExpect(jsonPath("$.data.newcomer").value(true))
                .andExpect(jsonPath("$.data.careTarget").value(true));

        Integer adminHistoryCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM registration_histories
                        WHERE registration_id = ?
                          AND actor_type = 'ADMIN'
                          AND actor_admin_user_id = 1
                          AND change_type IN (
                              'FEE_PAYMENT_UPDATED',
                              'STATUS_UPDATED',
                              'ADMIN_MANAGEMENT_UPDATED'
                          )
                        """,
                Integer.class,
                registrationId
        );
        assertThat(adminHistoryCount).isEqualTo(3);
    }

    @Test
    void staffCannotPerformParticipantManagementChanges() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long registrationId = created.path("data").path("registration").path("id").asLong();
        String staffToken = accessTokenForRole(AdminRole.STAFF);

        mockMvc.perform(patch("/api/admin/registrations/" + registrationId + "/fee-paid")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("feePaid", true))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/admin/registrations/" + registrationId + "/status")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CANCELLED"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/admin/registrations/" + registrationId + "/management")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "adminMemo", "No change",
                                "newcomer", true,
                                "careTarget", true
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void sensitiveAdminViewsCreatePrivacyAccessLogs() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long registrationId = created.path("data").path("registration").path("id").asLong();
        String staffToken = accessTokenForRole(AdminRole.STAFF);

        mockMvc.perform(get("/api/admin/registrations/" + registrationId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/registrations/" + registrationId + "/histories")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());

        Integer privacyLogCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM registration_privacy_access_logs
                        WHERE registration_id = ?
                          AND admin_user_id = 1
                          AND access_type IN ('DETAIL_VIEW', 'HISTORY_VIEW')
                        """,
                Integer.class,
                registrationId
        );
        String detailSensitiveFields = jdbcTemplate.queryForObject(
                """
                        SELECT sensitive_fields
                        FROM registration_privacy_access_logs
                        WHERE registration_id = ?
                          AND access_type = 'DETAIL_VIEW'
                        """,
                String.class,
                registrationId
        );

        assertThat(privacyLogCount).isEqualTo(2);
        assertThat(detailSensitiveFields).isEqualTo("phone_number");
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

    private String accessTokenForRole(AdminRole role) throws Exception {
        return signedToken(Map.of(
                "sub", "1",
                "email", "admin@gmc.local",
                "name", "System Admin",
                "role", role.name(),
                "iat", Instant.now().getEpochSecond(),
                "exp", Instant.now().plusSeconds(3600).getEpochSecond()
        ));
    }

    private MvcResult createRegistration(
            String name,
            String phoneNumber,
            String churchCellDepartment,
            boolean privacyConsentAgreed
    ) throws Exception {
        return mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest(name, phoneNumber, churchCellDepartment, privacyConsentAgreed)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String registrationRequest(
            String name,
            String phoneNumber,
            String churchCellDepartment,
            boolean privacyConsentAgreed
    ) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", name,
                "gender", "FEMALE",
                "birthYear", 1991,
                "phoneNumber", phoneNumber,
                "churchCellDepartment", churchCellDepartment,
                "privacyConsentAgreed", privacyConsentAgreed
        ));
    }

    private String selfUpdateRequest(String lookupKey, String phoneNumber) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", "Grace Kim",
                "phoneLastFour", "5678",
                "lookupKey", lookupKey,
                "update", Map.of(
                        "gender", "FEMALE",
                        "birthYear", 1992,
                        "phoneNumber", phoneNumber,
                        "churchCellDepartment", "Updated Cell"
                )
        ));
    }

    private int countOccurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
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
