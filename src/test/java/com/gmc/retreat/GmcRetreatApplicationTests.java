package com.gmc.retreat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import java.util.List;
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

    private static final String SENSITIVE_LOOKUP_JSON_FIELD = "lookup" + "Key" + "Hash";
    private static final String SENSITIVE_LOOKUP_DB_FIELD = "lookup_key" + "_hash";
    private static final String DEFAULT_LOOKUP_KEY = "123456";
    private static final String SENSITIVE_TOKEN_JSON_FIELD = "token" + "Hash";
    private static final String SENSITIVE_TOKEN_DB_FIELD = "token_" + "hash";

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
        jdbcTemplate.update("DELETE FROM participant_check_in_tokens");
        jdbcTemplate.update("DELETE FROM retreat_check_in_events");
        jdbcTemplate.update("DELETE FROM retreat_check_ins");
        jdbcTemplate.update("DELETE FROM registration_fee_events");
        jdbcTemplate.update("DELETE FROM registration_privacy_access_logs");
        jdbcTemplate.update("DELETE FROM registration_histories");
        jdbcTemplate.update("DELETE FROM announcement_targets");
        jdbcTemplate.update("DELETE FROM announcements");
        jdbcTemplate.update("DELETE FROM retreat_schedule_items");
        jdbcTemplate.update("DELETE FROM retreat_group_members");
        jdbcTemplate.update("DELETE FROM registrations");
        jdbcTemplate.update("DELETE FROM retreat_groups");
        jdbcTemplate.update("DELETE FROM church_cells");
        jdbcTemplate.update("DELETE FROM church_middle_groups");
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
    void appIdentityEndpointIsPublicAndReturnsConfiguredIdentity() throws Exception {
        mockMvc.perform(get("/api/app/identity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appName").value("Retreat Ops"))
                .andExpect(jsonPath("$.data.organizationName").value("Your Church"))
                .andExpect(jsonPath("$.data.eventName").value("Your Retreat"));
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
    void flywayMigrationCreatesCommunityTablesAndRegistrationChurchCellLink() {
        Integer middleGroupTableCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'church_middle_groups'
                        """,
                Integer.class
        );
        Integer cellTableCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'church_cells'
                        """,
                Integer.class
        );
        Integer registrationChurchCellColumnCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'registrations'
                          AND column_name = 'church_cell_id'
                        """,
                Integer.class
        );

        assertThat(middleGroupTableCount).isEqualTo(1);
        assertThat(cellTableCount).isEqualTo(1);
        assertThat(registrationChurchCellColumnCount).isEqualTo(1);
    }

    @Test
    void flywayMigrationCreatesRetreatGroupTables() {
        Integer groupTableCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'retreat_groups'
                        """,
                Integer.class
        );
        Integer memberTableCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'retreat_group_members'
                        """,
                Integer.class
        );

        assertThat(groupTableCount).isEqualTo(1);
        assertThat(memberTableCount).isEqualTo(1);
    }

    @Test
    void flywayMigrationCreatesAnnouncementTables() {
        Integer announcementTableCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'announcements'
                        """,
                Integer.class
        );
        Integer targetTableCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'announcement_targets'
                        """,
                Integer.class
        );

        assertThat(announcementTableCount).isEqualTo(1);
        assertThat(targetTableCount).isEqualTo(1);
    }

    @Test
    void flywayMigrationCreatesScheduleTable() {
        Integer scheduleTableCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'retreat_schedule_items'
                        """,
                Integer.class
        );

        assertThat(scheduleTableCount).isEqualTo(1);
    }

    @Test
    void flywayMigrationCreatesCheckInTables() {
        Integer currentStateTableCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'retreat_check_ins'
                        """,
                Integer.class
        );
        Integer eventTableCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'retreat_check_in_events'
                        """,
                Integer.class
        );
        Integer tokenTableCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'participant_check_in_tokens'
                        """,
                Integer.class
        );

        assertThat(currentStateTableCount).isEqualTo(1);
        assertThat(eventTableCount).isEqualTo(1);
        assertThat(tokenTableCount).isEqualTo(1);
    }

    @Test
    void flywayMigrationCreatesFeeManagementStructures() {
        Integer eventTableCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'registration_fee_events'
                        """,
                Integer.class
        );
        Integer updatedAtColumnCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'registrations'
                          AND column_name = 'fee_status_updated_at'
                        """,
                Integer.class
        );
        Integer updatedByColumnCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'registrations'
                          AND column_name = 'fee_status_updated_by_admin_id'
                        """,
                Integer.class
        );

        assertThat(eventTableCount).isEqualTo(1);
        assertThat(updatedAtColumnCount).isEqualTo(1);
        assertThat(updatedByColumnCount).isEqualTo(1);
    }

    @Test
    void bootstrapSystemAdminIsCreatedWhenMissingAndIsIdempotent() throws Exception {
        Integer adminCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_users WHERE email = 'admin@example.local'",
                Integer.class
        );
        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM admin_users WHERE email = 'admin@example.local'",
                String.class
        );

        assertThat(adminCount).isEqualTo(1);
        assertThat(passwordHash).isNotEqualTo("admin1234!");
        assertThat(passwordEncoder.matches("admin1234!", passwordHash)).isTrue();

        systemAdminBootstrapper.run(null);

        Integer adminCountAfterSecondRun = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_users WHERE email = 'admin@example.local'",
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
                                  "email": "admin@example.local",
                                  "password": "admin1234!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.admin.email").value("admin@example.local"))
                .andExpect(jsonPath("$.data.admin.role").value("SYSTEM_ADMIN"));
    }

    @Test
    void loginFailsWithInvalidPassword() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@example.local",
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
                .andExpect(jsonPath("$.data.email").value("admin@example.local"))
                .andExpect(jsonPath("$.data.name").value("System Admin"))
                .andExpect(jsonPath("$.data.role").value("SYSTEM_ADMIN"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void currentAdminProfileRejectsJwtWithInvalidSubjectClaim() throws Exception {
        String accessToken = signedToken(Map.of(
                "sub", "not-a-number",
                "email", "admin@example.local",
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
                "email", "admin@example.local",
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
                "email", "admin@example.local",
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
    void registrationCreationSucceedsAndNeverEchoesBackTheChosenLookupKey() throws Exception {
        MvcResult result = createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true);

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);

        assertThat(response.path("success").asBoolean()).isTrue();
        assertThat(response.path("data").path("resultType").asText()).isEqualTo("CREATED");
        assertThat(response.path("data").path("registration").path("name").asText()).isEqualTo("Grace Kim");
        assertThat(countOccurrences(responseBody, DEFAULT_LOOKUP_KEY)).isEqualTo(0);
    }

    @Test
    void databaseStoresBcryptLookupHashInsteadOfPlaintextLookupKey() throws Exception {
        createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true);

        String lookupHash = jdbcTemplate.queryForObject(
                "SELECT " + SENSITIVE_LOOKUP_DB_FIELD + " FROM registrations WHERE name = 'Grace Kim'",
                String.class
        );

        assertThat(lookupHash).isNotEqualTo(DEFAULT_LOOKUP_KEY);
        assertThat(lookupHash).startsWith("$2");
        assertThat(passwordEncoder.matches(DEFAULT_LOOKUP_KEY, lookupHash)).isTrue();
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
                                "lookupKey", DEFAULT_LOOKUP_KEY
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
                                "lookupKey", "999999"
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
                        .content(selfUpdateRequest(DEFAULT_LOOKUP_KEY, "010-9999-0000")))
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
                            .content(selfUpdateRequest(DEFAULT_LOOKUP_KEY, "010-9999-0000")))
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
    void fullAttendanceForcesAllScheduleSlotsAndLodgingTrue() throws Exception {
        MvcResult result = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of("attendDay1Morning", false, "lodgingNight1", false, "lodgingNight2", false)
        );
        result.getResponse().getContentAsString();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        SELECT lodging_night1, lodging_night2,
                               attend_day1_morning, attend_day1_afternoon, attend_day1_worship,
                               attend_day2_morning, attend_day2_afternoon, attend_day2_worship,
                               attend_day3_morning, attend_day3_afternoon
                        FROM registrations WHERE name = 'Grace Kim'
                        """
        );
        assertThat(row.values()).allSatisfy(value -> assertThat(value).isEqualTo(true));
    }

    @Test
    void worshipOnlyKeepsScheduleButForcesLodgingFalse() throws Exception {
        MvcResult result = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of(
                        "attendanceType", "WORSHIP_ONLY",
                        "transportation", "PUBLIC_TRANSIT",
                        "lodgingNight1", true,
                        "lodgingNight2", true,
                        "attendDay1Worship", true,
                        "attendDay2Worship", true
                )
        );
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        SELECT lodging_night1, lodging_night2, attend_day1_worship, attend_day2_worship,
                               attend_day1_morning
                        FROM registrations WHERE name = 'Grace Kim'
                        """
        );
        assertThat(row.get("lodging_night1")).isEqualTo(false);
        assertThat(row.get("lodging_night2")).isEqualTo(false);
        assertThat(row.get("attend_day1_worship")).isEqualTo(true);
        assertThat(row.get("attend_day2_worship")).isEqualTo(true);
        assertThat(row.get("attend_day1_morning")).isEqualTo(false);
    }

    @Test
    void partialAttendancePersistsScheduleAndLodgingAsSubmitted() throws Exception {
        MvcResult result = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of(
                        "attendanceType", "PARTIAL",
                        "transportation", "PUBLIC_TRANSIT",
                        "lodgingNight1", true,
                        "lodgingNight2", false,
                        "attendDay2Morning", true,
                        "attendDay2Afternoon", true
                )
        );
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        SELECT lodging_night1, lodging_night2, attend_day2_morning, attend_day2_afternoon,
                               attend_day1_morning
                        FROM registrations WHERE name = 'Grace Kim'
                        """
        );
        assertThat(row.get("lodging_night1")).isEqualTo(true);
        assertThat(row.get("lodging_night2")).isEqualTo(false);
        assertThat(row.get("attend_day2_morning")).isEqualTo(true);
        assertThat(row.get("attend_day2_afternoon")).isEqualTo(true);
        assertThat(row.get("attend_day1_morning")).isEqualTo(false);
    }

    @Test
    void fullAttendanceRejectsPublicTransit() throws Exception {
        MvcResult result = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of("attendanceType", "FULL", "transportation", "PUBLIC_TRANSIT")
        );
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(response.path("error").path("code").asText()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void partialAttendanceRejectsBus() throws Exception {
        MvcResult result = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of("attendanceType", "PARTIAL", "transportation", "BUS")
        );
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(response.path("error").path("code").asText()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void ownCarCarpoolAvailableRequiresSeatsWithinRange() throws Exception {
        MvcResult missingSeats = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of("transportation", "OWN_CAR", "carpoolAvailable", true)
        );
        assertThat(missingSeats.getResponse().getStatus()).isEqualTo(400);

        MvcResult outOfRange = createRegistration(
                "Grace Lee",
                "010-1234-9999",
                Map.of("transportation", "OWN_CAR", "carpoolAvailable", true, "carpoolSeats", 20)
        );
        assertThat(outOfRange.getResponse().getStatus()).isEqualTo(400);

        MvcResult valid = createRegistration(
                "Grace Park",
                "010-1234-8888",
                Map.of("transportation", "OWN_CAR", "carpoolAvailable", true, "carpoolSeats", 3)
        );
        assertThat(valid.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void carpoolFieldsRejectedWhenTransportationIsNotOwnCar() throws Exception {
        MvcResult result = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of("transportation", "BUS", "carpoolAvailable", true, "carpoolSeats", 2)
        );
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(response.path("error").path("code").asText()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void selfUpdateChangesAttendanceAnswersAndRecordsHistory() throws Exception {
        createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true);

        mockMvc.perform(put("/api/registrations/self")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selfUpdateRequest(
                                DEFAULT_LOOKUP_KEY,
                                "010-1234-5678",
                                Map.of(
                                        "attendanceType", "PARTIAL",
                                        "transportation", "PUBLIC_TRANSIT",
                                        "lodgingNight1", true,
                                        "attendDay1Morning", true
                                )
                        )))
                .andExpect(status().isOk());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT attendance_type, transportation_method, lodging_night1, attend_day1_morning "
                        + "FROM registrations WHERE name = 'Grace Kim'"
        );
        assertThat(row.get("attendance_type")).isEqualTo("PARTIAL");
        assertThat(row.get("transportation_method")).isEqualTo("PUBLIC_TRANSIT");
        assertThat(row.get("lodging_night1")).isEqualTo(true);
        assertThat(row.get("attend_day1_morning")).isEqualTo(true);

        Integer selfUpdatedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registration_histories WHERE change_type = 'SELF_UPDATED'",
                Integer.class
        );
        assertThat(selfUpdatedCount).isEqualTo(1);
    }

    @Test
    void historySnapshotIncludesAttendanceFields() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long registrationId = created.path("data").path("registration").path("id").asLong();
        String staffToken = accessTokenForRole(AdminRole.STAFF);

        MvcResult historiesResult = mockMvc.perform(get("/api/admin/registrations/" + registrationId + "/histories")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(historiesResult.getResponse().getContentAsString()).contains("attendanceType", "transportationMethod");
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
    void registrationResponsesNeverExposeSensitiveLookupFields() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long registrationId = created.path("data").path("registration").path("id").asLong();
        String accessToken = loginAndGetAccessToken();

        MvcResult lookupResult = mockMvc.perform(post("/api/registrations/self/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Grace Kim",
                                "lookupKey", DEFAULT_LOOKUP_KEY
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult updateResult = mockMvc.perform(put("/api/registrations/self")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selfUpdateRequest(DEFAULT_LOOKUP_KEY, "010-9999-0000")))
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

        assertNoSensitiveLookupFields(created.toString());
        assertNoSensitiveLookupFields(lookupResult.getResponse().getContentAsString());
        assertNoSensitiveLookupFields(updateResult.getResponse().getContentAsString());
        assertNoSensitiveLookupFields(adminListResult.getResponse().getContentAsString());
        assertNoSensitiveLookupFields(adminDetailResult.getResponse().getContentAsString());
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

    @Test
    void staffCanReadCommunityDataButCannotCreateOrUpdateIt() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long middleGroupId = createMiddleGroup(chairToken, "Alpha", "Elder A");
        Long cellId = createCell(chairToken, middleGroupId, "A1", "Leader A1");
        String staffToken = accessTokenForRole(AdminRole.STAFF);

        mockMvc.perform(get("/api/admin/community/middle-groups")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Alpha"));

        mockMvc.perform(get("/api/admin/community/cells/" + cellId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("A1"))
                .andExpect(jsonPath("$.data.middleGroupName").value("Alpha"));

        mockMvc.perform(post("/api/admin/community/middle-groups")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(middleGroupRequest("Beta", "Elder B", 1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/admin/community/cells/" + cellId)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cellRequest(middleGroupId, "A1 Updated", "Leader A1", 0)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void chairCanCreateAndPastorCanUpdateCommunityStructure() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        String pastorToken = accessTokenForRole(AdminRole.PASTOR);
        Long middleGroupId = createMiddleGroup(chairToken, "Alpha", "Elder A");
        Long cellId = createCell(chairToken, middleGroupId, "A1", "Leader A1");

        mockMvc.perform(patch("/api/admin/community/middle-groups/" + middleGroupId)
                        .header("Authorization", "Bearer " + pastorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(middleGroupRequest("Alpha Updated", "Elder A", 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Alpha Updated"))
                .andExpect(jsonPath("$.data.displayOrder").value(2));

        mockMvc.perform(patch("/api/admin/community/cells/" + cellId)
                        .header("Authorization", "Bearer " + pastorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cellRequest(middleGroupId, "A1 Updated", "Leader A2", 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("A1 Updated"))
                .andExpect(jsonPath("$.data.cellLeaderName").value("Leader A2"));

        mockMvc.perform(patch("/api/admin/community/middle-groups/" + middleGroupId + "/active")
                        .header("Authorization", "Bearer " + pastorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(patch("/api/admin/community/cells/" + cellId + "/active")
                        .header("Authorization", "Bearer " + pastorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void duplicateCommunityNamesFollowMiddleGroupAndCellRules() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long alphaId = createMiddleGroup(chairToken, "Alpha", "Elder A");
        Long betaId = createMiddleGroup(chairToken, "Beta", "Elder B");
        createCell(chairToken, alphaId, "Shared", "Leader A");

        mockMvc.perform(post("/api/admin/community/middle-groups")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(middleGroupRequest("Alpha", "Another Elder", 5)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_COMMUNITY_NAME"));

        mockMvc.perform(post("/api/admin/community/cells")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cellRequest(alphaId, "Shared", "Leader B", 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_COMMUNITY_NAME"));

        mockMvc.perform(post("/api/admin/community/cells")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cellRequest(betaId, "Shared", "Leader C", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.middleGroupId").value(betaId))
                .andExpect(jsonPath("$.data.name").value("Shared"));
    }

    @Test
    void communityTreeIncludesMiddleGroupsAndCells() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long middleGroupId = createMiddleGroup(chairToken, "Alpha", "Elder A");
        createCell(chairToken, middleGroupId, "A1", "Leader A1");
        String staffToken = accessTokenForRole(AdminRole.STAFF);

        mockMvc.perform(get("/api/admin/community/tree")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.middleGroups[0].name").value("Alpha"))
                .andExpect(jsonPath("$.data.middleGroups[0].cells[0].name").value("A1"))
                .andExpect(jsonPath("$.data.middleGroups[0].cells[0].cellLeaderName").value("Leader A1"));
    }

    @Test
    void chairCanLinkAndUnlinkParticipantChurchCellWithoutChangingFreeTextDepartment() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long participantId = created.path("data").path("registration").path("id").asLong();
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long middleGroupId = createMiddleGroup(chairToken, "Alpha", "Elder A");
        Long cellId = createCell(chairToken, middleGroupId, "A1", "Leader A1");

        mockMvc.perform(patch("/api/admin/participants/" + participantId + "/church-cell")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("churchCellId", cellId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.churchCellDepartment").value("Young Adults"))
                .andExpect(jsonPath("$.data.churchCellId").value(cellId))
                .andExpect(jsonPath("$.data.churchCellName").value("A1"))
                .andExpect(jsonPath("$.data.middleGroupId").value(middleGroupId))
                .andExpect(jsonPath("$.data.middleGroupName").value("Alpha"));

        mockMvc.perform(get("/api/admin/registrations")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].churchCellId").value(cellId))
                .andExpect(jsonPath("$.data.content[0].churchCellName").value("A1"))
                .andExpect(jsonPath("$.data.content[0].middleGroupName").value("Alpha"))
                .andExpect(jsonPath("$.data.content[0].churchCellDepartment").value("Young Adults"));

        mockMvc.perform(patch("/api/admin/participants/" + participantId + "/church-cell")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"churchCellId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.churchCellDepartment").value("Young Adults"))
                .andExpect(jsonPath("$.data.churchCellId").doesNotExist())
                .andExpect(jsonPath("$.data.churchCellName").doesNotExist());

        String freeTextDepartment = jdbcTemplate.queryForObject(
                "SELECT church_cell_department FROM registrations WHERE id = ?",
                String.class,
                participantId
        );
        assertThat(freeTextDepartment).isEqualTo("Young Adults");
    }

    @Test
    void staffCannotLinkParticipantChurchCellAndInvalidChurchCellFails() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long participantId = created.path("data").path("registration").path("id").asLong();
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        String staffToken = accessTokenForRole(AdminRole.STAFF);

        mockMvc.perform(patch("/api/admin/participants/" + participantId + "/church-cell")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("churchCellId", 999L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/admin/participants/" + participantId + "/church-cell")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("churchCellId", 999L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COMMUNITY_NOT_FOUND"));
    }

    @Test
    void staffCanReadRetreatGroupsButCannotCreateOrAssign() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long groupId = createRetreatGroup(chairToken, "Group 1");
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long participantId = created.path("data").path("registration").path("id").asLong();
        String staffToken = accessTokenForRole(AdminRole.STAFF);

        mockMvc.perform(get("/api/admin/retreat-groups")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Group 1"));

        mockMvc.perform(get("/api/admin/retreat-groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(post("/api/admin/retreat-groups")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(retreatGroupRequest("Group 2", 1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/admin/participants/" + participantId + "/retreat-group")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("retreatGroupId", groupId))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void chairCanCreateUpdateAndDeactivateRetreatGroup() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        String pastorToken = accessTokenForRole(AdminRole.PASTOR);
        Long groupId = createRetreatGroup(chairToken, "Group 1");

        mockMvc.perform(patch("/api/admin/retreat-groups/" + groupId)
                        .header("Authorization", "Bearer " + pastorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(retreatGroupRequest("Group 1 Updated", 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Group 1 Updated"))
                .andExpect(jsonPath("$.data.displayOrder").value(2));

        mockMvc.perform(patch("/api/admin/retreat-groups/" + groupId + "/active")
                        .header("Authorization", "Bearer " + pastorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(post("/api/admin/retreat-groups")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(retreatGroupRequest("Group 1 Updated", 3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RETREAT_GROUP_NAME"));
    }

    @Test
    void chairCanAssignAndRemoveParticipantRetreatGroup() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long participantId = created.path("data").path("registration").path("id").asLong();
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long groupId = createRetreatGroup(chairToken, "Group 1");

        mockMvc.perform(patch("/api/admin/participants/" + participantId + "/retreat-group")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("retreatGroupId", groupId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.churchCellDepartment").value("Young Adults"))
                .andExpect(jsonPath("$.data.retreatGroupId").value(groupId))
                .andExpect(jsonPath("$.data.retreatGroupName").value("Group 1"))
                .andExpect(jsonPath("$.data.retreatGroupLeader").value(false));

        mockMvc.perform(get("/api/admin/registrations")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].retreatGroupId").value(groupId))
                .andExpect(jsonPath("$.data.content[0].retreatGroupName").value("Group 1"))
                .andExpect(jsonPath("$.data.content[0].churchCellDepartment").value("Young Adults"));

        mockMvc.perform(get("/api/admin/retreat-groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].participantId").value(participantId))
                .andExpect(jsonPath("$.data[0].participantName").value("Grace Kim"))
                .andExpect(jsonPath("$.data[0].leader").value(false));

        mockMvc.perform(delete("/api/admin/participants/" + participantId + "/retreat-group")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("confirmText", "DELETE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retreatGroupId").doesNotExist())
                .andExpect(jsonPath("$.data.retreatGroupName").doesNotExist());
    }

    @Test
    void removingRetreatGroupAssignmentRequiresMatchingConfirmText() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long participantId = created.path("data").path("registration").path("id").asLong();
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long groupId = createRetreatGroup(chairToken, "Group 1");

        mockMvc.perform(patch("/api/admin/participants/" + participantId + "/retreat-group")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("retreatGroupId", groupId))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/admin/participants/" + participantId + "/retreat-group")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("confirmText", "delete"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("DELETE_CONFIRMATION_MISMATCH"));
    }

    @Test
    void duplicateRetreatGroupAssignmentIsRejected() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long participantId = created.path("data").path("registration").path("id").asLong();
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long groupOneId = createRetreatGroup(chairToken, "Group 1");
        Long groupTwoId = createRetreatGroup(chairToken, "Group 2");

        mockMvc.perform(patch("/api/admin/participants/" + participantId + "/retreat-group")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("retreatGroupId", groupOneId))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/participants/" + participantId + "/retreat-group")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("retreatGroupId", groupTwoId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RETREAT_GROUP_ASSIGNMENT"));
    }

    @Test
    void chairCanAssignAndRemoveRetreatGroupLeader() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long participantId = created.path("data").path("registration").path("id").asLong();
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long groupId = createRetreatGroup(chairToken, "Group 1");

        mockMvc.perform(patch("/api/admin/retreat-groups/" + groupId + "/leader")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("participantId", participantId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retreatGroupId").value(groupId))
                .andExpect(jsonPath("$.data.retreatGroupLeader").value(true));

        mockMvc.perform(get("/api/admin/retreat-groups/tree")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groups[0].name").value("Group 1"))
                .andExpect(jsonPath("$.data.groups[0].members[0].participantName").value("Grace Kim"))
                .andExpect(jsonPath("$.data.groups[0].members[0].leader").value(true));

        mockMvc.perform(delete("/api/admin/retreat-groups/" + groupId + "/leader")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("confirmText", "DELETE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].participantId").value(participantId))
                .andExpect(jsonPath("$.data[0].leader").value(false));
    }

    @Test
    void removingRetreatGroupLeaderRequiresMatchingConfirmText() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long participantId = created.path("data").path("registration").path("id").asLong();
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long groupId = createRetreatGroup(chairToken, "Group 1");

        mockMvc.perform(patch("/api/admin/retreat-groups/" + groupId + "/leader")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("participantId", participantId))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/admin/retreat-groups/" + groupId + "/leader")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("confirmText", "delete"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("DELETE_CONFIRMATION_MISMATCH"));
    }

    @Test
    void retreatGroupResponsesDoNotExposeSensitiveLookupFields() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long participantId = created.path("data").path("registration").path("id").asLong();
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long groupId = createRetreatGroup(chairToken, "Group 1");

        MvcResult assignResult = mockMvc.perform(patch("/api/admin/participants/" + participantId + "/retreat-group")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("retreatGroupId", groupId))))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult membersResult = mockMvc.perform(get("/api/admin/retreat-groups/" + groupId + "/members")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andReturn();

        assertNoSensitiveLookupFields(assignResult.getResponse().getContentAsString());
        assertNoSensitiveLookupFields(membersResult.getResponse().getContentAsString());
    }

    @Test
    void chairPastorAndSystemAdminCanCreateAnnouncements() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        String pastorToken = accessTokenForRole(AdminRole.PASTOR);
        String systemAdminToken = accessTokenForRole(AdminRole.SYSTEM_ADMIN);

        mockMvc.perform(post("/api/admin/announcements")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementRequest("Chair Notice", allTarget())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Chair Notice"))
                .andExpect(jsonPath("$.data.targets[0].targetType").value("ALL"));

        mockMvc.perform(post("/api/admin/announcements")
                        .header("Authorization", "Bearer " + pastorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementRequest("Pastor Notice", allTarget())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Pastor Notice"));

        mockMvc.perform(post("/api/admin/announcements")
                        .header("Authorization", "Bearer " + systemAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementRequest("System Notice", allTarget())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("System Notice"));
    }

    @Test
    void staffCanListAndDetailAnnouncementsButCannotChangeThem() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        String staffToken = accessTokenForRole(AdminRole.STAFF);
        Long announcementId = createAnnouncement(chairToken, "Staff Visible Notice", allTarget());

        mockMvc.perform(get("/api/admin/announcements")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Staff Visible Notice"));

        mockMvc.perform(get("/api/admin/announcements/" + announcementId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(announcementId));

        mockMvc.perform(post("/api/admin/announcements")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementRequest("Denied", allTarget())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/admin/announcements/" + announcementId + "/active")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void createAnnouncementSupportsRetreatGroupAndChurchTargets() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long retreatGroupId = createRetreatGroup(chairToken, "Group 1");
        Long middleGroupId = createMiddleGroup(chairToken, "Alpha", "Elder A");
        Long cellId = createCell(chairToken, middleGroupId, "A1", "Leader A1");

        mockMvc.perform(post("/api/admin/announcements")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementRequest(
                                "Targeted Notice",
                                List.of(
                                        target("RETREAT_GROUP", retreatGroupId.toString()),
                                        target("CHURCH_MIDDLE_GROUP", middleGroupId.toString()),
                                        target("CHURCH_CELL", cellId.toString())
                                )
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targets[0].targetType").value("RETREAT_GROUP"))
                .andExpect(jsonPath("$.data.targets[0].targetValue").value(retreatGroupId.toString()))
                .andExpect(jsonPath("$.data.targets[1].targetType").value("CHURCH_MIDDLE_GROUP"))
                .andExpect(jsonPath("$.data.targets[2].targetType").value("CHURCH_CELL"));
    }

    @Test
    void invalidAnnouncementVisiblePeriodIsRejected() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);

        mockMvc.perform(post("/api/admin/announcements")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Invalid Period",
                                "content", "Visible until is before visible from.",
                                "pinned", false,
                                "active", true,
                                "visibleFrom", "2026-07-02T00:00:00Z",
                                "visibleUntil", "2026-07-01T00:00:00Z",
                                "targets", allTarget()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void announcementActiveAndPinnedTogglesWork() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long announcementId = createAnnouncement(chairToken, "Toggle Notice", allTarget());

        mockMvc.perform(patch("/api/admin/announcements/" + announcementId + "/active")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(patch("/api/admin/announcements/" + announcementId + "/pinned")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pinned", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pinned").value(true));
    }

    @Test
    void updateAnnouncementReplacesTargets() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long retreatGroupId = createRetreatGroup(chairToken, "Group 1");
        Long announcementId = createAnnouncement(chairToken, "Original Notice", allTarget());

        mockMvc.perform(patch("/api/admin/announcements/" + announcementId)
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementRequest(
                                "Updated Notice",
                                List.of(
                                        target("RETREAT_GROUP", retreatGroupId.toString()),
                                        target("PAYMENT_STATUS", "PAID"),
                                        target("NEWCOMER", "TRUE")
                                )
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Notice"))
                .andExpect(jsonPath("$.data.targets.length()").value(3))
                .andExpect(jsonPath("$.data.targets[0].targetType").value("RETREAT_GROUP"))
                .andExpect(jsonPath("$.data.targets[1].targetType").value("PAYMENT_STATUS"))
                .andExpect(jsonPath("$.data.targets[2].targetType").value("NEWCOMER"));

        Integer allTargetCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM announcement_targets
                        WHERE announcement_id = ?
                          AND target_type = 'ALL'
                        """,
                Integer.class,
                announcementId
        );
        assertThat(allTargetCount).isZero();
    }

    @Test
    void duplicateAnnouncementTargetsAreRejected() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);

        mockMvc.perform(post("/api/admin/announcements")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementRequest(
                                "Duplicate Targets",
                                List.of(
                                        target("REGISTRATION_STATUS", "REGISTERED"),
                                        target("REGISTRATION_STATUS", "registered")
                                )
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_ANNOUNCEMENT_TARGET"));
    }

    @Test
    void announcementResponsesDoNotExposeSensitiveLookupFields() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long announcementId = createAnnouncement(chairToken, "Privacy Notice", allTarget());

        MvcResult listResult = mockMvc.perform(get("/api/admin/announcements")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult detailResult = mockMvc.perform(get("/api/admin/announcements/" + announcementId)
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andReturn();

        assertNoSensitiveLookupFields(listResult.getResponse().getContentAsString());
        assertNoSensitiveLookupFields(detailResult.getResponse().getContentAsString());
    }

    @Test
    void chairPastorAndSystemAdminCanCreateScheduleItems() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        String pastorToken = accessTokenForRole(AdminRole.PASTOR);
        String systemAdminToken = accessTokenForRole(AdminRole.SYSTEM_ADMIN);

        mockMvc.perform(post("/api/admin/schedules")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleRequest("Chair Opening Worship", "WORSHIP", "ALL", true, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Chair Opening Worship"))
                .andExpect(jsonPath("$.data.category").value("WORSHIP"))
                .andExpect(jsonPath("$.data.targetAudience").value("ALL"));

        mockMvc.perform(post("/api/admin/schedules")
                        .header("Authorization", "Bearer " + pastorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleRequest("Pastor Lecture", "LECTURE", "LEADERS_ONLY", true, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Pastor Lecture"));

        mockMvc.perform(post("/api/admin/schedules")
                        .header("Authorization", "Bearer " + systemAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleRequest("System Notice Time", "NOTICE", "STAFF_ONLY", true, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("System Notice Time"));
    }

    @Test
    void staffCanListAndDetailScheduleItemsButCannotChangeThem() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        String staffToken = accessTokenForRole(AdminRole.STAFF);
        Long scheduleId = createSchedule(chairToken, "Staff Readable Schedule", "MEAL", "ALL", true, 0);

        mockMvc.perform(get("/api/admin/schedules")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Staff Readable Schedule"));

        mockMvc.perform(get("/api/admin/schedules/" + scheduleId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(scheduleId));

        mockMvc.perform(post("/api/admin/schedules")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleRequest("Denied Schedule", "NOTICE", "ALL", true, 1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/admin/schedules/" + scheduleId)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleRequest("Denied Update", "NOTICE", "ALL", true, 1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/admin/schedules/" + scheduleId + "/active")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void invalidScheduleTimeRangeIsRejected() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);

        mockMvc.perform(post("/api/admin/schedules")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Invalid Range",
                                "description", "Ends before starts.",
                                "scheduleDate", "2026-07-01",
                                "startsAt", "2026-07-01T11:00:00Z",
                                "endsAt", "2026-07-01T10:00:00Z",
                                "location", "Chapel",
                                "category", "WORSHIP",
                                "targetAudience", "ALL",
                                "active", true,
                                "displayOrder", 0
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void scheduleDateDifferentFromStartsAtDateIsRejected() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);

        mockMvc.perform(post("/api/admin/schedules")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Invalid Start Date",
                                "description", "Schedule date does not match startsAt.",
                                "scheduleDate", "2026-07-02",
                                "startsAt", "2026-07-01T09:00:00Z",
                                "endsAt", "2026-07-01T10:00:00Z",
                                "location", "Chapel",
                                "category", "WORSHIP",
                                "targetAudience", "ALL",
                                "active", true,
                                "displayOrder", 0
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void scheduleEndingOnDifferentDateIsRejected() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);

        mockMvc.perform(post("/api/admin/schedules")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Cross Date Schedule",
                                "description", "Cross-date schedule items are not allowed in MVP.",
                                "scheduleDate", "2026-07-01",
                                "startsAt", "2026-07-01T23:30:00Z",
                                "endsAt", "2026-07-02T00:30:00Z",
                                "location", "Chapel",
                                "category", "PRAYER",
                                "targetAudience", "ALL",
                                "active", true,
                                "displayOrder", 0
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void scheduleActiveToggleAndUpdateWork() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long scheduleId = createSchedule(chairToken, "Original Schedule", "BREAK", "ALL", true, 0);

        mockMvc.perform(patch("/api/admin/schedules/" + scheduleId + "/active")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(patch("/api/admin/schedules/" + scheduleId)
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleRequest("Updated Schedule", "GROUP_ACTIVITY", "NEWCOMERS", true, 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Schedule"))
                .andExpect(jsonPath("$.data.category").value("GROUP_ACTIVITY"))
                .andExpect(jsonPath("$.data.targetAudience").value("NEWCOMERS"))
                .andExpect(jsonPath("$.data.displayOrder").value(3));
    }

    @Test
    void scheduleListFilteringByDateCategoryAndActiveWorks() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        createSchedule(chairToken, "Visible Meal", "MEAL", "ALL", true, 0);
        createSchedule(chairToken, "Inactive Meal", "MEAL", "ALL", false, 1);
        createSchedule(chairToken, "Visible Prayer", "PRAYER", "ALL", true, 2);

        mockMvc.perform(get("/api/admin/schedules")
                        .param("date", "2026-07-01")
                        .param("category", "MEAL")
                        .param("active", "true")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Visible Meal"))
                .andExpect(jsonPath("$.data[0].category").value("MEAL"))
                .andExpect(jsonPath("$.data[0].active").value(true));
    }

    @Test
    void scheduleResponsesDoNotExposeSensitiveLookupFields() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long scheduleId = createSchedule(chairToken, "Privacy Schedule", "NOTICE", "ALL", true, 0);

        MvcResult listResult = mockMvc.perform(get("/api/admin/schedules")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult detailResult = mockMvc.perform(get("/api/admin/schedules/" + scheduleId)
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andReturn();

        assertNoSensitiveLookupFields(listResult.getResponse().getContentAsString());
        assertNoSensitiveLookupFields(detailResult.getResponse().getContentAsString());
    }

    @Test
    void staffCanViewCheckInRosterAndManuallyCheckInParticipant() throws Exception {
        String staffToken = accessTokenForRole(AdminRole.STAFF);
        Long participantId = createParticipant("Grace Kim", "010-1111-1234");

        MvcResult rosterBeforeCheckIn = mockMvc.perform(get("/api/admin/check-ins")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].participantId").value(participantId))
                .andExpect(jsonPath("$.data.content[0].phoneLast4").value("1234"))
                .andExpect(jsonPath("$.data.content[0].checkedIn").value(false))
                .andExpect(jsonPath("$.data.content[0].phoneNumber").doesNotExist())
                .andReturn();

        MvcResult checkInResult = mockMvc.perform(post("/api/admin/check-ins/" + participantId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participantId").value(participantId))
                .andExpect(jsonPath("$.data.checkedIn").value(true))
                .andExpect(jsonPath("$.data.checkInMethod").value("MANUAL"))
                .andExpect(jsonPath("$.data.checkedInBy.id").value(1))
                .andReturn();

        Integer eventCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM retreat_check_in_events
                        WHERE participant_id = ?
                          AND action = 'CHECKED_IN'
                          AND method = 'MANUAL'
                        """,
                Integer.class,
                participantId
        );

        assertThat(eventCount).isEqualTo(1);
        assertNoSensitiveCheckInFields(rosterBeforeCheckIn.getResponse().getContentAsString());
        assertNoSensitiveCheckInFields(checkInResult.getResponse().getContentAsString());
    }

    @Test
    void duplicateManualCheckInIsRejected() throws Exception {
        String staffToken = accessTokenForRole(AdminRole.STAFF);
        Long participantId = createParticipant("Noah Park", "010-2222-5678");

        mockMvc.perform(post("/api/admin/check-ins/" + participantId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/check-ins/" + participantId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CHECK_IN_ALREADY_COMPLETED"));

        Integer checkedInEventCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM retreat_check_in_events
                        WHERE participant_id = ?
                          AND action = 'CHECKED_IN'
                        """,
                Integer.class,
                participantId
        );

        assertThat(checkedInEventCount).isEqualTo(1);
    }

    @Test
    void staffCannotCancelCheckInButChairCanCancelWithReason() throws Exception {
        String staffToken = accessTokenForRole(AdminRole.STAFF);
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long participantId = createParticipant("Eun Lee", "010-3333-9012");

        mockMvc.perform(post("/api/admin/check-ins/" + participantId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/check-ins/" + participantId + "/cancel")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInCancellationRequest("Wrong participant.")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        MvcResult cancelResult = mockMvc.perform(patch("/api/admin/check-ins/" + participantId + "/cancel")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInCancellationRequest("Wrong participant.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedIn").value(false))
                .andExpect(jsonPath("$.data.cancelledBy.id").value(1))
                .andReturn();

        Integer eventCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM retreat_check_in_events
                        WHERE participant_id = ?
                          AND action = 'CANCELLED'
                        """,
                Integer.class,
                participantId
        );

        assertThat(eventCount).isEqualTo(1);
        assertNoSensitiveCheckInFields(cancelResult.getResponse().getContentAsString());
    }

    @Test
    void duplicateCancellationIsRejectedWithoutDuplicateEvent() throws Exception {
        String staffToken = accessTokenForRole(AdminRole.STAFF);
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long participantId = createParticipant("Cancel Once Person", "010-3333-1122");

        mockMvc.perform(post("/api/admin/check-ins/" + participantId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/check-ins/" + participantId + "/cancel")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInCancellationRequest("Initial cancellation.")))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/check-ins/" + participantId + "/cancel")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInCancellationRequest("Duplicate cancellation.")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CHECK_IN_NOT_COMPLETED"));

        Integer cancelledEventCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM retreat_check_in_events
                        WHERE participant_id = ?
                          AND action = 'CANCELLED'
                        """,
                Integer.class,
                participantId
        );

        assertThat(cancelledEventCount).isEqualTo(1);
    }

    @Test
    void pastorCanCancelCheckInAndCancellationReasonIsRequired() throws Exception {
        String staffToken = accessTokenForRole(AdminRole.STAFF);
        String pastorToken = accessTokenForRole(AdminRole.PASTOR);
        Long participantId = createParticipant("Mina Choi", "010-4444-3456");

        mockMvc.perform(post("/api/admin/check-ins/" + participantId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/check-ins/" + participantId + "/cancel")
                        .header("Authorization", "Bearer " + pastorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInCancellationRequest(" ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CHECK_IN_CANCELLATION_REASON_REQUIRED"));

        mockMvc.perform(patch("/api/admin/check-ins/" + participantId + "/cancel")
                        .header("Authorization", "Bearer " + pastorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInCancellationRequest("Duplicate registration resolved.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedIn").value(false));
    }

    @Test
    void checkInRosterSupportsFilters() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long middleGroupId = createMiddleGroup(chairToken, "Check In Alpha", "Elder A");
        Long cellId = createCell(chairToken, middleGroupId, "Check In A1", "Leader A1");
        Long groupId = createRetreatGroup(chairToken, "Check In Group 1");
        Long participantId = createParticipant("Filter Person", "010-5555-7890");
        mockMvc.perform(patch("/api/admin/participants/" + participantId + "/church-cell")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("churchCellId", cellId))))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/admin/participants/" + participantId + "/retreat-group")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("retreatGroupId", groupId))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/check-ins/" + participantId)
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/check-ins")
                        .param("checkedIn", "true")
                        .param("retreatGroupId", groupId.toString())
                        .param("churchCellId", cellId.toString())
                        .param("keyword", "7890")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].participantId").value(participantId))
                .andExpect(jsonPath("$.data.content[0].retreatGroupId").value(groupId))
                .andExpect(jsonPath("$.data.content[0].churchCellId").value(cellId));
    }

    @Test
    void chairCanIssueAndRevokeQrTokenWithoutExposingStoredHash() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long participantId = createParticipant("Token Person", "010-6666-1234");

        MvcResult issueResult = mockMvc.perform(post("/api/admin/check-ins/tokens/" + participantId)
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInTokenIssueRequest("2099-07-01T00:00:00Z")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participantId").value(participantId))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data." + SENSITIVE_TOKEN_JSON_FIELD).doesNotExist())
                .andReturn();

        JsonNode response = objectMapper.readTree(issueResult.getResponse().getContentAsString());
        String rawToken = response.path("data").path("token").asText();
        String storedTokenHash = jdbcTemplate.queryForObject(
                "SELECT " + SENSITIVE_TOKEN_DB_FIELD + " FROM participant_check_in_tokens WHERE participant_id = ?",
                String.class,
                participantId
        );

        assertThat(rawToken).isNotBlank();
        assertThat(storedTokenHash).isNotBlank();
        assertThat(storedTokenHash).isNotEqualTo(rawToken);

        MvcResult revokeResult = mockMvc.perform(patch("/api/admin/check-ins/tokens/" + participantId + "/revoke")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participantId").value(participantId))
                .andExpect(jsonPath("$.data.revokedAt").exists())
                .andExpect(jsonPath("$.data." + SENSITIVE_TOKEN_JSON_FIELD).doesNotExist())
                .andReturn();

        assertNoSensitiveCheckInFields(issueResult.getResponse().getContentAsString());
        assertNoSensitiveCheckInFields(revokeResult.getResponse().getContentAsString());
    }

    @Test
    void issuingQrTokenRevokesPreviousActiveParticipantTokens() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long participantId = createParticipant("Rotating Token Person", "010-6666-5678");

        MvcResult firstIssueResult = mockMvc.perform(post("/api/admin/check-ins/tokens/" + participantId)
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInTokenIssueRequest("2099-07-01T00:00:00Z")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data." + SENSITIVE_TOKEN_JSON_FIELD).doesNotExist())
                .andReturn();

        MvcResult secondIssueResult = mockMvc.perform(post("/api/admin/check-ins/tokens/" + participantId)
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInTokenIssueRequest("2099-07-02T00:00:00Z")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data." + SENSITIVE_TOKEN_JSON_FIELD).doesNotExist())
                .andReturn();

        Integer activeTokenCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM participant_check_in_tokens
                        WHERE participant_id = ?
                          AND revoked_at IS NULL
                          AND expires_at > now()
                        """,
                Integer.class,
                participantId
        );
        Integer revokedTokenCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM participant_check_in_tokens
                        WHERE participant_id = ?
                          AND revoked_at IS NOT NULL
                        """,
                Integer.class,
                participantId
        );

        assertThat(activeTokenCount).isEqualTo(1);
        assertThat(revokedTokenCount).isEqualTo(1);
        assertNoSensitiveCheckInFields(firstIssueResult.getResponse().getContentAsString());
        assertNoSensitiveCheckInFields(secondIssueResult.getResponse().getContentAsString());
    }

    @Test
    void staffCannotIssueOrRevokeQrToken() throws Exception {
        String staffToken = accessTokenForRole(AdminRole.STAFF);
        Long participantId = createParticipant("Denied Token Person", "010-7777-1234");

        mockMvc.perform(post("/api/admin/check-ins/tokens/" + participantId)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInTokenIssueRequest("2099-07-01T00:00:00Z")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/admin/check-ins/tokens/" + participantId + "/revoke")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void staffCanViewFeeRosterAndDetailWithoutSensitiveFields() throws Exception {
        String staffToken = accessTokenForRole(AdminRole.STAFF);
        Long participantId = createParticipant("Fee Roster Person", "010-1111-2468");

        MvcResult rosterResult = mockMvc.perform(get("/api/admin/fees")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].participantId").value(participantId))
                .andExpect(jsonPath("$.data.content[0].phoneLast4").value("2468"))
                .andExpect(jsonPath("$.data.content[0].feePaid").value(false))
                .andExpect(jsonPath("$.data.content[0].phoneNumber").doesNotExist())
                .andReturn();

        MvcResult detailResult = mockMvc.perform(get("/api/admin/fees/" + participantId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participant.participantId").value(participantId))
                .andExpect(jsonPath("$.data.participant.phoneLast4").value("2468"))
                .andExpect(jsonPath("$.data.participant.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.data.events.length()").value(0))
                .andReturn();

        assertNoSensitiveCheckInFields(rosterResult.getResponse().getContentAsString());
        assertNoSensitiveCheckInFields(detailResult.getResponse().getContentAsString());
    }

    @Test
    void feeRosterSupportsFeePaidFilter() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long paidParticipantId = createParticipant("Paid Filter Person", "010-2222-2468");
        createParticipant("Unpaid Filter Person", "010-2222-1357");

        mockMvc.perform(patch("/api/admin/fees/" + paidParticipantId)
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feeStatusRequest(true, "Confirmed by treasurer.")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/fees")
                        .param("feePaid", "true")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].participantId").value(paidParticipantId))
                .andExpect(jsonPath("$.data.content[0].feePaid").value(true));
    }

    @Test
    void staffCannotUpdateFeeStatus() throws Exception {
        String staffToken = accessTokenForRole(AdminRole.STAFF);
        Long participantId = createParticipant("Fee Denied Person", "010-3333-2468");

        mockMvc.perform(patch("/api/admin/fees/" + participantId)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feeStatusRequest(true, "Confirmed by treasurer.")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void chairCanMarkUnpaidParticipantAsPaidAndCreatesEvent() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long participantId = createParticipant("Fee Paid Person", "010-4444-2468");

        MvcResult updateResult = mockMvc.perform(patch("/api/admin/fees/" + participantId)
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feeStatusRequest(true, "Confirmed by treasurer.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participant.participantId").value(participantId))
                .andExpect(jsonPath("$.data.participant.feePaid").value(true))
                .andExpect(jsonPath("$.data.participant.feeStatusUpdatedAt").exists())
                .andExpect(jsonPath("$.data.participant.feeStatusUpdatedBy.id").value(1))
                .andExpect(jsonPath("$.data.events[0].previousFeePaid").value(false))
                .andExpect(jsonPath("$.data.events[0].newFeePaid").value(true))
                .andReturn();

        Integer eventCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM registration_fee_events
                        WHERE registration_id = ?
                          AND previous_fee_paid = FALSE
                          AND new_fee_paid = TRUE
                          AND changed_by_admin_id = 1
                        """,
                Integer.class,
                participantId
        );

        assertThat(eventCount).isEqualTo(1);
        assertNoSensitiveCheckInFields(updateResult.getResponse().getContentAsString());
    }

    @Test
    void chairCanRevertPaidParticipantToUnpaidWithReason() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long participantId = createParticipant("Fee Revert Person", "010-5555-2468");

        mockMvc.perform(patch("/api/admin/fees/" + participantId)
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feeStatusRequest(true, "Confirmed by treasurer.")))
                .andExpect(status().isOk());

        MvcResult revertResult = mockMvc.perform(patch("/api/admin/fees/" + participantId)
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feeStatusRequest(false, "Marked paid by mistake.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participant.feePaid").value(false))
                .andExpect(jsonPath("$.data.events[0].previousFeePaid").value(true))
                .andExpect(jsonPath("$.data.events[0].newFeePaid").value(false))
                .andExpect(jsonPath("$.data.events[0].reason").value("Marked paid by mistake."))
                .andReturn();

        Integer eventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registration_fee_events WHERE registration_id = ?",
                Integer.class,
                participantId
        );

        assertThat(eventCount).isEqualTo(2);
        assertNoSensitiveCheckInFields(revertResult.getResponse().getContentAsString());
    }

    @Test
    void revertingFeeToUnpaidRequiresReason() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long participantId = createParticipant("Fee Reason Person", "010-6666-2468");

        mockMvc.perform(patch("/api/admin/fees/" + participantId)
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feeStatusRequest(true, "Confirmed by treasurer.")))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/fees/" + participantId)
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feeStatusRequest(false, " ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("FEE_REVERT_REASON_REQUIRED"));
    }

    @Test
    void duplicatePaidUpdateIsRejectedWithoutDuplicateEvent() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long participantId = createParticipant("Duplicate Paid Person", "010-7777-2468");

        mockMvc.perform(patch("/api/admin/fees/" + participantId)
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feeStatusRequest(true, "Confirmed by treasurer.")))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/fees/" + participantId)
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feeStatusRequest(true, "Duplicate confirmation.")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("FEE_ALREADY_PAID"));

        Integer eventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registration_fee_events WHERE registration_id = ?",
                Integer.class,
                participantId
        );

        assertThat(eventCount).isEqualTo(1);
    }

    @Test
    void duplicateUnpaidUpdateIsRejectedWithoutEvent() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long participantId = createParticipant("Duplicate Unpaid Person", "010-8888-2468");

        mockMvc.perform(patch("/api/admin/fees/" + participantId)
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feeStatusRequest(false, "Already unpaid.")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("FEE_ALREADY_UNPAID"));

        Integer eventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registration_fee_events WHERE registration_id = ?",
                Integer.class,
                participantId
        );

        assertThat(eventCount).isZero();
    }

    @Test
    void feeDetailMissingRegistrationReturnsBusinessError() throws Exception {
        String staffToken = accessTokenForRole(AdminRole.STAFF);

        mockMvc.perform(get("/api/admin/fees/999999")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REGISTRATION_NOT_FOUND"));
    }

    @Test
    void participantSelfLookupExposesOnlyOwnFeePaid() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        JsonNode firstCreated = objectMapper.readTree(
                createRegistration("Self Fee One", "010-9999-2468", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long firstParticipantId = firstCreated.path("data").path("registration").path("id").asLong();
        JsonNode secondCreated = objectMapper.readTree(
                createRegistration("Self Fee Two", "010-9999-1357", "Young Adults", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long secondParticipantId = secondCreated.path("data").path("registration").path("id").asLong();

        mockMvc.perform(patch("/api/admin/fees/" + firstParticipantId)
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feeStatusRequest(true, "Confirmed by treasurer.")))
                .andExpect(status().isOk());

        MvcResult lookupResult = mockMvc.perform(post("/api/registrations/self/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Self Fee One",
                                "lookupKey", DEFAULT_LOOKUP_KEY
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(firstParticipantId))
                .andExpect(jsonPath("$.data.feePaid").value(true))
                .andReturn();

        assertThat(secondParticipantId).isNotEqualTo(firstParticipantId);
        assertThat(lookupResult.getResponse().getContentAsString())
                .doesNotContain("Self Fee Two", "1357");
        assertNoSensitiveCheckInFields(lookupResult.getResponse().getContentAsString());
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
                                  "email": "admin@example.local",
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
                "email", "admin@example.local",
                "name", "System Admin",
                "role", role.name(),
                "iat", Instant.now().getEpochSecond(),
                "exp", Instant.now().plusSeconds(3600).getEpochSecond()
        ));
    }

    private Long createMiddleGroup(String accessToken, String name, String elderName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/community/middle-groups")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(middleGroupRequest(name, elderName, 0)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("id").asLong();
    }

    private Long createCell(String accessToken, Long middleGroupId, String name, String cellLeaderName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/community/cells")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cellRequest(middleGroupId, name, cellLeaderName, 0)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("id").asLong();
    }

    private Long createRetreatGroup(String accessToken, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/retreat-groups")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(retreatGroupRequest(name, 0)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("id").asLong();
    }

    private Long createAnnouncement(
            String accessToken,
            String title,
            List<Map<String, String>> targets
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/announcements")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementRequest(title, targets)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("id").asLong();
    }

    private Long createSchedule(
            String accessToken,
            String title,
            String category,
            String targetAudience,
            boolean active,
            int displayOrder
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/schedules")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleRequest(title, category, targetAudience, active, displayOrder)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("id").asLong();
    }

    private Long createParticipant(String name, String phoneNumber) throws Exception {
        MvcResult result = createRegistration(name, phoneNumber, "Young Adults", true);
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("registration").path("id").asLong();
    }

    private String middleGroupRequest(String name, String elderName, int displayOrder) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", name,
                "elderName", elderName,
                "description", name + " description",
                "displayOrder", displayOrder
        ));
    }

    private String cellRequest(Long middleGroupId, String name, String cellLeaderName, int displayOrder) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "middleGroupId", middleGroupId,
                "name", name,
                "cellLeaderName", cellLeaderName,
                "description", name + " description",
                "displayOrder", displayOrder
        ));
    }

    private String retreatGroupRequest(String name, int displayOrder) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", name,
                "description", name + " description",
                "displayOrder", displayOrder
        ));
    }

    private String announcementRequest(String title, List<Map<String, String>> targets) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "title", title,
                "content", title + " content",
                "pinned", false,
                "active", true,
                "visibleFrom", "2026-07-01T00:00:00Z",
                "visibleUntil", "2026-07-31T23:59:59Z",
                "targets", targets
        ));
    }

    private String scheduleRequest(
            String title,
            String category,
            String targetAudience,
            boolean active,
            int displayOrder
    ) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "title", title,
                "description", title + " description",
                "scheduleDate", "2026-07-01",
                "startsAt", "2026-07-01T09:00:00Z",
                "endsAt", "2026-07-01T10:00:00Z",
                "location", "Main Chapel",
                "category", category,
                "targetAudience", targetAudience,
                "active", active,
                "displayOrder", displayOrder
        ));
    }

    private String checkInCancellationRequest(String reason) throws Exception {
        return objectMapper.writeValueAsString(Map.of("reason", reason));
    }

    private String checkInTokenIssueRequest(String expiresAt) throws Exception {
        return objectMapper.writeValueAsString(Map.of("expiresAt", expiresAt));
    }

    private String feeStatusRequest(boolean feePaid, String reason) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "feePaid", feePaid,
                "reason", reason
        ));
    }

    private List<Map<String, String>> allTarget() {
        return List.of(Map.of("targetType", "ALL"));
    }

    private Map<String, String> target(String targetType, String targetValue) {
        return Map.of(
                "targetType", targetType,
                "targetValue", targetValue
        );
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

    private MvcResult createRegistration(
            String name,
            String phoneNumber,
            Map<String, Object> attendanceOverrides
    ) throws Exception {
        return mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest(name, phoneNumber, attendanceOverrides)))
                .andReturn();
    }

    private String registrationRequest(
            String name,
            String phoneNumber,
            String churchCellDepartment,
            boolean privacyConsentAgreed
    ) throws Exception {
        return objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("name", name),
                Map.entry("gender", "FEMALE"),
                Map.entry("birthYear", 1991),
                Map.entry("phoneNumber", phoneNumber),
                Map.entry("churchCellDepartment", churchCellDepartment),
                Map.entry("privacyConsentAgreed", privacyConsentAgreed),
                Map.entry("lookupKey", DEFAULT_LOOKUP_KEY),
                Map.entry("attendanceType", "FULL"),
                Map.entry("transportation", "BUS")
        ));
    }

    private String registrationRequest(
            String name,
            String phoneNumber,
            Map<String, Object> attendanceOverrides
    ) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("gender", "FEMALE");
        body.put("birthYear", 1991);
        body.put("phoneNumber", phoneNumber);
        body.put("churchCellDepartment", "Young Adults");
        body.put("privacyConsentAgreed", true);
        body.put("lookupKey", DEFAULT_LOOKUP_KEY);
        body.put("attendanceType", "FULL");
        body.put("transportation", "BUS");
        body.putAll(attendanceOverrides);
        return objectMapper.writeValueAsString(body);
    }

    private String selfUpdateRequest(String lookupKey, String phoneNumber) throws Exception {
        return selfUpdateRequest(lookupKey, phoneNumber, Map.of());
    }

    private String selfUpdateRequest(
            String lookupKey,
            String phoneNumber,
            Map<String, Object> attendanceOverrides
    ) throws Exception {
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("gender", "FEMALE");
        update.put("birthYear", 1992);
        update.put("phoneNumber", phoneNumber);
        update.put("churchCellDepartment", "Updated Cell");
        update.put("attendanceType", "FULL");
        update.put("transportation", "BUS");
        update.putAll(attendanceOverrides);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Grace Kim");
        body.put("phoneLastFour", "5678");
        body.put("lookupKey", lookupKey);
        body.put("update", update);
        return objectMapper.writeValueAsString(body);
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

    private void assertNoSensitiveLookupFields(String value) {
        assertThat(value).doesNotContain(SENSITIVE_LOOKUP_JSON_FIELD, SENSITIVE_LOOKUP_DB_FIELD);
    }

    private void assertNoSensitiveCheckInFields(String value) {
        assertThat(value).doesNotContain(
                SENSITIVE_LOOKUP_JSON_FIELD,
                SENSITIVE_LOOKUP_DB_FIELD,
                SENSITIVE_TOKEN_JSON_FIELD,
                SENSITIVE_TOKEN_DB_FIELD
        );
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
