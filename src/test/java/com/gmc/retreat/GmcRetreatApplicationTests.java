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
        jdbcTemplate.update("DELETE FROM retreat_group_members");
        jdbcTemplate.update("DELETE FROM registration_participation_options");
        jdbcTemplate.update("DELETE FROM registrations");
        jdbcTemplate.update("DELETE FROM retreat_participation_options");
        jdbcTemplate.update("DELETE FROM retreat_schedule_items");
        jdbcTemplate.update("DELETE FROM retreat_groups");
        jdbcTemplate.update("DELETE FROM church_cells");
        jdbcTemplate.update("DELETE FROM church_middle_groups");
        jdbcTemplate.update("DELETE FROM retreats");
        jdbcTemplate.update("""
                INSERT INTO retreats (name, starts_on, ends_on, status, registration_open)
                VALUES ('Test Retreat', DATE '2026-08-14', DATE '2026-08-16', 'OPEN', TRUE)
                """);
        jdbcTemplate.update("""
                INSERT INTO retreat_participation_options
                    (retreat_id, option_type, label, event_date, display_order)
                SELECT id, option_type, label, event_date, display_order
                FROM retreats
                CROSS JOIN (VALUES
                    ('PROGRAM', '오후 프로그램', DATE '2026-08-14', 10),
                    ('MEAL', '저녁식사', DATE '2026-08-14', 20),
                    ('PROGRAM', '집회', DATE '2026-08-14', 30),
                    ('MEAL', '아침식사', DATE '2026-08-15', 40),
                    ('PROGRAM', '오전 프로그램', DATE '2026-08-15', 50),
                    ('MEAL', '점심식사', DATE '2026-08-15', 60),
                    ('PROGRAM', '오후 프로그램', DATE '2026-08-15', 70),
                    ('PROGRAM', '집회', DATE '2026-08-15', 80),
                    ('MEAL', '아침식사', DATE '2026-08-16', 90),
                    ('PROGRAM', '오전 프로그램', DATE '2026-08-16', 100),
                    ('MEAL', '점심식사', DATE '2026-08-16', 110),
                    ('PROGRAM', '오후 프로그램', DATE '2026-08-16', 120)
                ) AS defaults(option_type, label, event_date, display_order)
                WHERE status = 'OPEN'
                """);
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
    void flywayMigrationCreatesDynamicParticipationOptionTables() {
        assertThat(tableExists("retreat_participation_options")).isTrue();
        assertThat(tableExists("registration_participation_options")).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM retreat_participation_options",
                Integer.class
        )).isEqualTo(12);
    }

    @Test
    void appIdentityEndpointIsPublicAndReturnsConfiguredIdentity() throws Exception {
        mockMvc.perform(get("/api/app/identity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appName").value("청년2부 수련회"))
                .andExpect(jsonPath("$.data.organizationName").value("지구촌교회 드림공동체 청년2부"))
                .andExpect(jsonPath("$.data.eventName").value("Test Retreat"))
                .andExpect(jsonPath("$.data.registrationOpen").value(true));
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
    void flywayMigrationCreatesRegistrationAffiliationSnapshotColumns() {
        Integer affiliationColumnCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'registrations'
                          AND column_name IN ('middle_group_name', 'cell_name')
                        """,
                Integer.class
        );

        assertThat(affiliationColumnCount).isEqualTo(2);
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
        assertThat(jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND (table_name, column_name) IN (
                              ('retreat_schedule_items', 'collect_participation'),
                              ('retreat_participation_options', 'schedule_item_id')
                          )
                        """,
                Integer.class
        )).isEqualTo(2);
    }

    @Test
    void flywayMigrationCreatesRetreatFoundationAndCurrentRetreat() {
        Integer retreatTableCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'retreats'
                        """,
                Integer.class
        );
        Integer retreatLinkCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name IN (
                              'registrations',
                              'retreat_groups',
                              'announcements',
                              'retreat_schedule_items'
                          )
                          AND column_name = 'retreat_id'
                        """,
                Integer.class
        );
        Integer currentRetreatCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM retreats WHERE status IN ('DRAFT', 'OPEN')",
                Integer.class
        );

        assertThat(retreatTableCount).isEqualTo(1);
        assertThat(retreatLinkCount).isEqualTo(4);
        assertThat(currentRetreatCount).isEqualTo(1);
    }

    @Test
    void retreatLifecycleClosesWithSummaryAndIsolatesTheNextRetreat() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long currentRetreatId = jdbcTemplate.queryForObject(
                "SELECT id FROM retreats WHERE status = 'OPEN'",
                Long.class
        );
        Long previousParticipantId = createParticipant("Repeat Participant", "010-1234-5678");
        Long cancelledParticipantId = createParticipant("Cancelled Participant", "010-1234-0000");
        mockMvc.perform(patch("/api/admin/registrations/" + cancelledParticipantId + "/status")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CANCELLED"}
                                """))
                .andExpect(status().isOk());
        Long previousScheduleId = createSchedule(
                chairToken,
                "Previous Retreat Worship",
                "WORSHIP",
                "ALL",
                true,
                0
        );

        mockMvc.perform(patch("/api/admin/retreats/" + currentRetreatId + "/status")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CLOSED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.participantCount").value(1));

        Long retainedScheduleRetreatId = jdbcTemplate.queryForObject(
                "SELECT retreat_id FROM retreat_schedule_items WHERE id = ?",
                Long.class,
                previousScheduleId
        );
        assertThat(retainedScheduleRetreatId).isEqualTo(currentRetreatId);

        mockMvc.perform(get("/api/admin/schedules")
                        .header("Authorization", "Bearer " + chairToken)
                        .param("retreatId", currentRetreatId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(previousScheduleId))
                .andExpect(jsonPath("$.data[0].title").value("Previous Retreat Worship"));

        mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest(
                                "Closed Registration",
                                "010-9999-0000",
                                "Young Adults",
                                true
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REGISTRATION_NOT_OPEN"));

        MvcResult createdRetreatResult = mockMvc.perform(post("/api/admin/retreats")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"2027 Winter Retreat",
                                  "startsOn":"2027-01-15",
                                  "endsOn":"2027-01-17"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        Long nextRetreatId = objectMapper.readTree(createdRetreatResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest(
                                "Draft Registration",
                                "010-9999-1111",
                                "Young Adults",
                                true
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REGISTRATION_NOT_OPEN"));

        mockMvc.perform(patch("/api/admin/retreats/" + nextRetreatId + "/status")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"OPEN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        Long nextParticipantId = createParticipant("Repeat Participant", "010-1234-5678");
        assertThat(nextParticipantId).isNotEqualTo(previousParticipantId);

        mockMvc.perform(get("/api/admin/registrations/" + previousParticipantId)
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REGISTRATION_NOT_FOUND"));
    }

    @Test
    void closingNewApplicationsStillAllowsParticipantUpdatesDuringOperations() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long retreatId = jdbcTemplate.queryForObject(
                "SELECT id FROM retreats WHERE status = 'OPEN'",
                Long.class
        );
        createRegistration("Grace Kim", "010-1234-5678", "Young Adults", true);

        mockMvc.perform(patch("/api/admin/retreats/" + retreatId + "/registration-open")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("registrationOpen", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.registrationOpen").value(false));

        mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest("New Applicant", "010-7777-9999", "Young Adults", true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REGISTRATION_NOT_OPEN"));

        mockMvc.perform(put("/api/registrations/self")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(selfUpdateRequest(DEFAULT_LOOKUP_KEY, "010-1234-5678")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cellName").value("Updated Cell"));

        mockMvc.perform(get("/api/admin/registrations")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].participantUpdatedAt").exists());

        mockMvc.perform(get("/api/app/identity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registrationOpen").value(false));
    }

    @Test
    void staffCanReadRetreatsButCannotChangeLifecycle() throws Exception {
        String staffToken = accessTokenForRole(AdminRole.STAFF);

        mockMvc.perform(get("/api/admin/retreats")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("OPEN"));

        mockMvc.perform(post("/api/admin/retreats")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Forbidden Retreat",
                                  "startsOn":"2027-03-01",
                                  "endsOn":"2027-03-03"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void cannotCreateAnotherRetreatWhileOneIsCurrent() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);

        mockMvc.perform(post("/api/admin/retreats")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Overlapping Retreat",
                                  "startsOn":"2027-02-01",
                                  "endsOn":"2027-02-03"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CURRENT_RETREAT_ALREADY_EXISTS"));
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
        assertThat(duplicate.path("data").path("registration").path("cellName").asText())
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
                .andExpect(jsonPath("$.data.middleGroupName").value("Updated Middle Group"))
                .andExpect(jsonPath("$.data.cellName").value("Updated Cell"));

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
    void fullAttendanceSelectsAllActiveOptionsAndForcesLodgingTrue() throws Exception {
        MvcResult result = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of("selectedOptionIds", List.of(), "lodgingNight1", false, "lodgingNight2", false)
        );
        result.getResponse().getContentAsString();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT id, lodging_night1, lodging_night2 FROM registrations WHERE name = 'Grace Kim'"
        );
        assertThat(row.get("lodging_night1")).isEqualTo(true);
        assertThat(row.get("lodging_night2")).isEqualTo(true);
        Integer selectionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registration_participation_options WHERE registration_id = ?",
                Integer.class,
                row.get("id")
        );
        assertThat(selectionCount).isEqualTo(12);
    }

    @Test
    void worshipOnlyKeepsSelectedOptionsButForcesLodgingFalse() throws Exception {
        MvcResult result = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of(
                        "attendanceType", "WORSHIP_ONLY",
                        "inboundTransportationMethod", "PUBLIC_TRANSIT",
                        "outboundTransportationMethod", "PUBLIC_TRANSIT",
                        "lodgingNight1", true,
                        "lodgingNight2", true,
                        "selectedOptionIds", List.of(
                                participationOptionId("2026-08-14", "집회"),
                                participationOptionId("2026-08-15", "집회")
                        )
                )
        );
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT id, lodging_night1, lodging_night2 FROM registrations WHERE name = 'Grace Kim'"
        );
        assertThat(row.get("lodging_night1")).isEqualTo(false);
        assertThat(row.get("lodging_night2")).isEqualTo(false);
        Integer selectionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registration_participation_options WHERE registration_id = ?",
                Integer.class,
                row.get("id")
        );
        assertThat(selectionCount).isEqualTo(2);
    }

    @Test
    void partialAttendancePersistsSelectedOptionsAndLodgingAsSubmitted() throws Exception {
        MvcResult result = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of(
                        "attendanceType", "PARTIAL",
                        "transportation", "PUBLIC_TRANSIT",
                        "lodgingNight1", true,
                        "lodgingNight2", false,
                        "selectedOptionIds", List.of(
                                participationOptionId("2026-08-15", "오전 프로그램"),
                                participationOptionId("2026-08-15", "오후 프로그램")
                        )
                )
        );
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT id, lodging_night1, lodging_night2 FROM registrations WHERE name = 'Grace Kim'"
        );
        assertThat(row.get("lodging_night1")).isEqualTo(true);
        assertThat(row.get("lodging_night2")).isEqualTo(false);
        Integer selectionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registration_participation_options WHERE registration_id = ?",
                Integer.class,
                row.get("id")
        );
        assertThat(selectionCount).isEqualTo(2);
    }

    @Test
    void partialAttendanceRequiresArrivalAndDepartureTimeRange() throws Exception {
        Map<String, Object> missingTimes = new LinkedHashMap<>();
        missingTimes.put("name", "Grace Kim");
        missingTimes.put("gender", "FEMALE");
        missingTimes.put("birthYear", 1991);
        missingTimes.put("phoneNumber", "010-1234-5678");
        missingTimes.put("middleGroupName", "Dream");
        missingTimes.put("cellName", "Young Adults");
        missingTimes.put("privacyConsentAgreed", true);
        missingTimes.put("lookupKey", DEFAULT_LOOKUP_KEY);
        missingTimes.put("attendanceType", "PARTIAL");
        missingTimes.put("inboundTransportationMethod", "PUBLIC_TRANSIT");
        missingTimes.put("outboundTransportationMethod", "PUBLIC_TRANSIT");

        mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingTimes)))
                .andExpect(status().isBadRequest());

        MvcResult invalidRange = createRegistration(
                "Grace Lee",
                "010-1234-9999",
                Map.of(
                        "attendanceType", "PARTIAL",
                        "plannedArrivalAt", "2026-08-02T21:00:00+09:00",
                        "plannedDepartureAt", "2026-08-02T19:00:00+09:00",
                        "inboundTransportationMethod", "PUBLIC_TRANSIT",
                        "outboundTransportationMethod", "PUBLIC_TRANSIT"
                )
        );
        assertThat(invalidRange.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void partialAttendanceWorshipBusRequiresDirectionSpecificRideSlot() throws Exception {
        MvcResult missingSlot = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of(
                        "attendanceType", "PARTIAL",
                        "inboundTransportationMethod", "WORSHIP_SHUTTLE",
                        "outboundTransportationMethod", "PUBLIC_TRANSIT"
                )
        );
        assertThat(missingSlot.getResponse().getStatus()).isEqualTo(400);

        MvcResult wrongDirectionSlot = createRegistration(
                "Grace Lee",
                "010-1234-9999",
                Map.of(
                        "attendanceType", "PARTIAL",
                        "inboundTransportationMethod", "WORSHIP_SHUTTLE",
                        "inboundWorshipBusRideSlot", "DAY1_AFTER_WORSHIP",
                        "outboundTransportationMethod", "PUBLIC_TRANSIT"
                )
        );
        assertThat(wrongDirectionSlot.getResponse().getStatus()).isEqualTo(400);

        MvcResult valid = createRegistration(
                "Grace Han",
                "010-1234-7777",
                Map.of(
                        "attendanceType", "PARTIAL",
                        "inboundTransportationMethod", "WORSHIP_SHUTTLE",
                        "inboundWorshipBusRideSlot", "DAY2_BEFORE_WORSHIP",
                        "outboundTransportationMethod", "WORSHIP_SHUTTLE",
                        "outboundWorshipBusRideSlot", "DAY2_AFTER_WORSHIP"
                )
        );
        assertThat(valid.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void worshipOnlyCanUseWorshipBusWithRideSlots() throws Exception {
        MvcResult result = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of(
                        "attendanceType", "WORSHIP_ONLY",
                        "inboundTransportationMethod", "WORSHIP_SHUTTLE",
                        "inboundWorshipBusRideSlot", "DAY1_BEFORE_WORSHIP",
                        "outboundTransportationMethod", "WORSHIP_SHUTTLE",
                        "outboundWorshipBusRideSlot", "DAY1_AFTER_WORSHIP",
                        "attendDay1Worship", true
                )
        );
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void undecidedTransportationIsRejectedForNewApplications() throws Exception {
        MvcResult result = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of(
                        "attendanceType", "WORSHIP_ONLY",
                        "selectedOptionIds", List.of(),
                        "inboundTransportationMethod", "NOT_DECIDED",
                        "outboundTransportationMethod", "NOT_DECIDED"
                )
        );

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("error").path("code").asText()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void ownCarRequiresCarpoolAvailable() throws Exception {
        MvcResult result = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of("inboundTransportationMethod", "OWN_CAR")
        );
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(response.path("error").path("code").asText()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void carpoolNeededRejectsSeatsField() throws Exception {
        MvcResult result = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of("inboundTransportationMethod", "CARPOOL_NEEDED", "inboundCarpoolSeats", 2)
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
                Map.of(
                        "inboundTransportationMethod", "OWN_CAR",
                        "outboundTransportationMethod", "OWN_CAR",
                        "inboundCarpoolAvailable", true,
                        "inboundCarpoolArea", "Gangnam",
                        "outboundCarpoolAvailable", false
                )
        );
        assertThat(missingSeats.getResponse().getStatus()).isEqualTo(400);

        MvcResult outOfRange = createRegistration(
                "Grace Lee",
                "010-1234-9999",
                Map.of(
                        "inboundTransportationMethod", "OWN_CAR",
                        "outboundTransportationMethod", "OWN_CAR",
                        "inboundCarpoolAvailable", true,
                        "inboundCarpoolSeats", 20,
                        "inboundCarpoolArea", "Gangnam",
                        "outboundCarpoolAvailable", false
                )
        );
        assertThat(outOfRange.getResponse().getStatus()).isEqualTo(400);

        MvcResult missingArea = createRegistration(
                "Grace Han",
                "010-1234-7777",
                Map.of(
                        "inboundTransportationMethod", "OWN_CAR",
                        "outboundTransportationMethod", "OWN_CAR",
                        "inboundCarpoolAvailable", true,
                        "inboundCarpoolSeats", 3,
                        "outboundCarpoolAvailable", false
                )
        );
        assertThat(missingArea.getResponse().getStatus()).isEqualTo(400);

        MvcResult valid = createRegistration(
                "Grace Park",
                "010-1234-8888",
                Map.of(
                        "inboundTransportationMethod", "OWN_CAR",
                        "outboundTransportationMethod", "OWN_CAR",
                        "inboundCarpoolAvailable", true,
                        "inboundCarpoolSeats", 3,
                        "inboundCarpoolArea", "Gangnam",
                        "inboundCarpoolNote", "Can stop by Jamsil",
                        "outboundCarpoolAvailable", false
                )
        );
        assertThat(valid.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void fullAttendanceRejectsOneWayOwnCarCombinations() throws Exception {
        MvcResult ownCarOutboundBus = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of(
                        "inboundTransportationMethod", "OWN_CAR",
                        "outboundTransportationMethod", "GROUP_BUS",
                        "inboundCarpoolAvailable", false
                )
        );
        assertThat(ownCarOutboundBus.getResponse().getStatus()).isEqualTo(400);

        MvcResult busOutboundOwnCar = createRegistration(
                "Grace Lee",
                "010-1234-9999",
                Map.of(
                        "inboundTransportationMethod", "GROUP_BUS",
                        "outboundTransportationMethod", "OWN_CAR",
                        "outboundCarpoolAvailable", false
                )
        );
        assertThat(busOutboundOwnCar.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void partialAttendanceRejectsOneWayOwnCarCombinations() throws Exception {
        MvcResult ownCarThenBus = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of(
                        "attendanceType", "PARTIAL",
                        "inboundTransportationMethod", "OWN_CAR",
                        "outboundTransportationMethod", "GROUP_BUS",
                        "inboundCarpoolAvailable", false
                )
        );
        assertThat(ownCarThenBus.getResponse().getStatus()).isEqualTo(400);

        MvcResult busThenOwnCar = createRegistration(
                "Grace Lee",
                "010-1234-9999",
                Map.of(
                        "attendanceType", "PARTIAL",
                        "inboundTransportationMethod", "WORSHIP_SHUTTLE",
                        "inboundWorshipBusRideSlot", "DAY1_BEFORE_WORSHIP",
                        "outboundTransportationMethod", "OWN_CAR",
                        "outboundCarpoolAvailable", false
                )
        );
        assertThat(busThenOwnCar.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void fullAttendanceAcceptsSeparatedNonOwnCarTransportation() throws Exception {
        MvcResult outboundCarpool = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of(
                        "inboundTransportationMethod", "GROUP_BUS",
                        "outboundTransportationMethod", "CARPOOL_NEEDED",
                        "outboundCarpoolPreferredArea", "Seoul Station",
                        "outboundCarpoolPreferredNote", "Can get off near church"
                )
        );
        assertThat(outboundCarpool.getResponse().getStatus()).isEqualTo(200);

        MvcResult inboundCarpool = createRegistration(
                "Grace Lee",
                "010-1234-9999",
                Map.of(
                        "inboundTransportationMethod", "CARPOOL_NEEDED",
                        "inboundCarpoolPreferredArea", "Gangnam Station",
                        "inboundCarpoolPreferredNote", "Available after 7pm",
                        "outboundTransportationMethod", "PUBLIC_TRANSIT"
                )
        );
        assertThat(inboundCarpool.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void fullAttendanceOwnCarAllowsDirectionalCarpoolOfferDetails() throws Exception {
        MvcResult result = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of(
                        "inboundTransportationMethod", "OWN_CAR",
                        "outboundTransportationMethod", "OWN_CAR",
                        "inboundCarpoolAvailable", true,
                        "inboundCarpoolSeats", 2,
                        "inboundCarpoolArea", "Church",
                        "inboundCarpoolNote", "Can stop by Jamsil",
                        "outboundCarpoolAvailable", true,
                        "outboundCarpoolSeats", 1,
                        "outboundCarpoolArea", "Seoul Station",
                        "outboundCarpoolNote", "Large luggage, one seat only"
                )
        );
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        SELECT inbound_carpool_seats, inbound_carpool_area, inbound_carpool_note,
                               outbound_carpool_seats, outbound_carpool_area, outbound_carpool_note
                        FROM registrations WHERE name = 'Grace Kim'
                        """
        );
        assertThat(row.get("inbound_carpool_seats")).isEqualTo(2);
        assertThat(row.get("inbound_carpool_area")).isEqualTo("Church");
        assertThat(row.get("inbound_carpool_note")).isEqualTo("Can stop by Jamsil");
        assertThat(row.get("outbound_carpool_seats")).isEqualTo(1);
        assertThat(row.get("outbound_carpool_area")).isEqualTo("Seoul Station");
        assertThat(row.get("outbound_carpool_note")).isEqualTo("Large luggage, one seat only");
    }

    @Test
    void carpoolNeededRequiresPreferredArea() throws Exception {
        MvcResult missingPreferredArea = createRegistration(
                "Grace Choi",
                "010-1234-6666",
                Map.of("inboundTransportationMethod", "CARPOOL_NEEDED")
        );
        assertThat(missingPreferredArea.getResponse().getStatus()).isEqualTo(400);

        MvcResult valid = createRegistration(
                "Grace Yoon",
                "010-1234-4444",
                Map.of("inboundTransportationMethod", "CARPOOL_NEEDED", "inboundCarpoolPreferredArea", "Gangnam")
        );
        assertThat(valid.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void carpoolOfferFieldsRejectedWhenParticipantDoesNotUseOwnCar() throws Exception {
        MvcResult result = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of(
                        "attendanceType", "PARTIAL",
                        "inboundTransportationMethod", "PUBLIC_TRANSIT",
                        "outboundTransportationMethod", "PUBLIC_TRANSIT",
                        "outboundCarpoolAvailable", true,
                        "outboundCarpoolSeats", 2,
                        "outboundCarpoolArea", "Church"
                )
        );
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void carpoolFieldsRejectedWhenTransportationIsNotOwnCar() throws Exception {
        MvcResult result = createRegistration(
                "Grace Kim",
                "010-1234-5678",
                Map.of("inboundTransportationMethod", "GROUP_BUS", "inboundCarpoolAvailable", true, "inboundCarpoolSeats", 2)
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
                                        "inboundTransportationMethod", "PUBLIC_TRANSIT",
                                        "outboundTransportationMethod", "PUBLIC_TRANSIT",
                                        "lodgingNight1", true,
                                        "selectedOptionIds", List.of(
                                                participationOptionId("2026-08-14", "오후 프로그램")
                                        )
                                )
                        )))
                .andExpect(status().isOk());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT id, attendance_type, inbound_transportation_method, lodging_night1 "
                        + "FROM registrations WHERE name = 'Grace Kim'"
        );
        assertThat(row.get("attendance_type")).isEqualTo("PARTIAL");
        assertThat(row.get("inbound_transportation_method")).isEqualTo("PUBLIC_TRANSIT");
        assertThat(row.get("lodging_night1")).isEqualTo(true);
        Integer selectionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registration_participation_options WHERE registration_id = ?",
                Integer.class,
                row.get("id")
        );
        assertThat(selectionCount).isEqualTo(1);

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

        assertThat(historiesResult.getResponse().getContentAsString()).contains("attendanceType", "inboundTransportationMethod", "outboundTransportationMethod");
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
    void adminRegistrationListSupportsOperationalFiltersAndMaskedRosterFields() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long checkedParticipantId = createParticipant("Alpha Checked", "010-1111-1234");
        Long taggedParticipantId = createParticipant("Beta Pending", "010-2222-5678");
        JsonNode partialCreated = objectMapper.readTree(
                createRegistration("Gamma Partial", "010-3333-9012", Map.of(
                        "attendanceType", "PARTIAL",
                        "lodgingNight1", true,
                        "lodgingNight2", false,
                        "inboundTransportationMethod", "CARPOOL_NEEDED",
                        "inboundCarpoolPreferredArea", "Downtown",
                        "outboundTransportationMethod", "PUBLIC_TRANSIT"
                )).getResponse().getContentAsString()
        );
        Long partialParticipantId = partialCreated.path("data").path("registration").path("id").asLong();

        mockMvc.perform(post("/api/admin/check-ins/" + checkedParticipantId)
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/admin/registrations/" + checkedParticipantId + "/fee-paid")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("feePaid", true))))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/admin/registrations/" + taggedParticipantId + "/management")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "adminMemo", "Follow up",
                                "newcomer", true,
                                "careTarget", false
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/registrations")
                        .param("keyword", "Alpha")
                        .param("checkedIn", "true")
                        .param("feePaid", "true")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(checkedParticipantId))
                .andExpect(jsonPath("$.data.content[0].checkedIn").value(true))
                .andExpect(jsonPath("$.data.content[0].phoneNumber").value("010****1234"))
                .andExpect(jsonPath("$.data.content[0].phoneNumber").value(org.hamcrest.Matchers.not("01011111234")));

        mockMvc.perform(get("/api/admin/registrations")
                        .param("keyword", "Beta")
                        .param("checkedIn", "false")
                        .param("newcomer", "true")
                        .param("retreatGroupAssigned", "false")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(taggedParticipantId))
                .andExpect(jsonPath("$.data.content[0].checkedIn").value(false))
                .andExpect(jsonPath("$.data.content[0].newcomer").value(true));

        mockMvc.perform(get("/api/admin/registrations")
                        .param("keyword", "Gamma")
                        .param("attendanceType", "PARTIAL")
                        .param("transportationNeed", "CARPOOL_NEEDED")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(partialParticipantId))
                .andExpect(jsonPath("$.data.content[0].attendanceType").value("PARTIAL"))
                .andExpect(jsonPath("$.data.content[0].lodgingNight1").value(true))
                .andExpect(jsonPath("$.data.content[0].plannedArrivalAt").value(org.hamcrest.Matchers.nullValue()));
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
        assertThat(detailSensitiveFields).isEqualTo("phone_number,transportation_carpool_fields");
    }

    @Test
    void registrationStoresAffiliationNamesWithoutCommunityMasterData() throws Exception {
        JsonNode created = objectMapper.readTree(
                createRegistration("Grace Kim", "010-1234-5678", "Joy Cell", true)
                        .getResponse()
                        .getContentAsString()
        );
        Long participantId = created.path("data").path("registration").path("id").asLong();

        assertThat(created.path("data").path("registration").path("middleGroupName").asText())
                .isEqualTo("Dream");
        assertThat(created.path("data").path("registration").path("cellName").asText())
                .isEqualTo("Joy Cell");
        Map<String, Object> affiliation = jdbcTemplate.queryForMap(
                "SELECT middle_group_name, cell_name FROM registrations WHERE id = ?",
                participantId
        );
        assertThat(affiliation.get("middle_group_name")).isEqualTo("Dream");
        assertThat(affiliation.get("cell_name")).isEqualTo("Joy Cell");
    }

    @Test
    void legacyCommunityAdminEndpointIsRemoved() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);

        mockMvc.perform(get("/api/admin/community/tree")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminRegistrationFiltersUseEnteredAffiliationNames() throws Exception {
        createRegistration("Grace Kim", "010-1234-5678", "Joy Cell", true);
        createRegistration("Hope Lee", "010-2222-3333", "", true);
        String staffToken = accessTokenForRole(AdminRole.STAFF);

        mockMvc.perform(get("/api/admin/registrations")
                        .param("keyword", "Joy Cell")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].middleGroupName").value("Dream"))
                .andExpect(jsonPath("$.data.content[0].cellName").value("Joy Cell"));

        mockMvc.perform(get("/api/admin/registrations")
                        .param("cellAssigned", "false")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Hope Lee"));
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
    void chairCanDeleteRetreatGroupAndReuseName() throws Exception {
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

        mockMvc.perform(delete("/api/admin/retreat-groups/" + groupId)
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("confirmText", "DELETE"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/retreat-groups")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        mockMvc.perform(get("/api/admin/registrations")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].retreatGroupId").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].retreatGroupName").doesNotExist());

        mockMvc.perform(post("/api/admin/retreat-groups")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(retreatGroupRequest("Group 1", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Group 1"));
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
                .andExpect(jsonPath("$.data.middleGroupName").value("Dream"))
                .andExpect(jsonPath("$.data.cellName").value("Young Adults"))
                .andExpect(jsonPath("$.data.retreatGroupId").value(groupId))
                .andExpect(jsonPath("$.data.retreatGroupName").value("Group 1"))
                .andExpect(jsonPath("$.data.retreatGroupLeader").value(false));

        mockMvc.perform(get("/api/admin/registrations")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].retreatGroupId").value(groupId))
                .andExpect(jsonPath("$.data.content[0].retreatGroupName").value("Group 1"))
                .andExpect(jsonPath("$.data.content[0].middleGroupName").value("Dream"))
                .andExpect(jsonPath("$.data.content[0].cellName").value("Young Adults"));

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
    void assignedParticipantCanMoveToAnotherRetreatGroup() throws Exception {
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.retreatGroupId").value(groupTwoId))
                .andExpect(jsonPath("$.data.retreatGroupName").value("Group 2"))
                .andExpect(jsonPath("$.data.retreatGroupLeader").value(false));

        mockMvc.perform(get("/api/admin/retreat-groups/" + groupOneId + "/members")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        mockMvc.perform(get("/api/admin/retreat-groups/" + groupTwoId + "/members")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].participantId").value(participantId));
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
    void createAnnouncementSupportsRetreatGroupAndRejectsRemovedCommunityTargets() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long retreatGroupId = createRetreatGroup(chairToken, "Group 1");

        mockMvc.perform(post("/api/admin/announcements")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementRequest(
                                "Targeted Notice",
                                List.of(target("RETREAT_GROUP", retreatGroupId.toString()))
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targets[0].targetType").value("RETREAT_GROUP"))
                .andExpect(jsonPath("$.data.targets[0].targetValue").value(retreatGroupId.toString()));

        mockMvc.perform(post("/api/admin/announcements")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementRequest(
                                "Removed Community Target",
                                List.of(target("CHURCH_CELL", "1"))
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
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
    void publicParticipationOptionsAreAvailableOnlyAsPublicFields() throws Exception {
        mockMvc.perform(get("/api/participation-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(12))
                .andExpect(jsonPath("$.data[0].label").value("오후 프로그램"))
                .andExpect(jsonPath("$.data[0].selectionCount").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdAt").doesNotExist());
    }

    @Test
    void staffCanReadParticipationOptionsButCannotChangeThem() throws Exception {
        String staffToken = accessTokenForRole(AdminRole.STAFF);

        mockMvc.perform(get("/api/admin/participation-options")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(12));

        mockMvc.perform(post("/api/admin/participation-options")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(participationOptionRequest("MEAL", "야식", "2026-08-16", 999, true)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void chairCanManageParticipationOptionsAndSeeSelectionCount() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        MvcResult createdOption = mockMvc.perform(post("/api/admin/participation-options")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(participationOptionRequest("MEAL", "야식", "2026-08-16", 999, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectionCount").value(0))
                .andReturn();
        Long optionId = objectMapper.readTree(createdOption.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        MvcResult registration = createRegistration(
                "Meal Counter",
                "010-7777-8888",
                Map.of(
                        "attendanceType", "PARTIAL",
                        "inboundTransportationMethod", "PUBLIC_TRANSIT",
                        "outboundTransportationMethod", "PUBLIC_TRANSIT",
                        "selectedOptionIds", List.of(optionId)
                )
        );
        assertThat(registration.getResponse().getStatus()).isEqualTo(200);

        mockMvc.perform(get("/api/admin/participation-options")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[12].label").value("야식"))
                .andExpect(jsonPath("$.data[12].selectionCount").value(1));

        mockMvc.perform(patch("/api/admin/participation-options/" + optionId + "/active")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(get("/api/participation-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(12));
    }

    @Test
    void participationOptionsRejectOutsideDatesAndDuplicateLabels() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);

        mockMvc.perform(post("/api/admin/participation-options")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(participationOptionRequest("MEAL", "외부 식사", "2026-08-17", 1, true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/admin/participation-options")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(participationOptionRequest("PROGRAM", "오후 프로그램", "2026-08-14", 999, true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_PARTICIPATION_OPTION"));
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
                                "scheduleDate", "2026-08-14",
                                "startsAt", "2026-08-14T11:00:00+09:00",
                                "endsAt", "2026-08-14T10:00:00+09:00",
                                "location", "Chapel",
                                "category", "WORSHIP",
                                "targetAudience", "ALL",
                                "active", true,
                                "displayOrder", 0,
                                "collectParticipation", false
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
                                "scheduleDate", "2026-08-15",
                                "startsAt", "2026-08-14T09:00:00+09:00",
                                "endsAt", "2026-08-14T10:00:00+09:00",
                                "location", "Chapel",
                                "category", "WORSHIP",
                                "targetAudience", "ALL",
                                "active", true,
                                "displayOrder", 0,
                                "collectParticipation", false
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
                                "scheduleDate", "2026-08-14",
                                "startsAt", "2026-08-14T23:30:00+09:00",
                                "endsAt", "2026-08-15T00:30:00+09:00",
                                "location", "Chapel",
                                "category", "PRAYER",
                                "targetAudience", "ALL",
                                "active", true,
                                "displayOrder", 0,
                                "collectParticipation", false
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
    void scheduleCanCollectParticipationAndPreserveSelectionsWhenHidden() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);

        MvcResult created = mockMvc.perform(post("/api/admin/schedules")
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleRequestWithParticipation(
                                "선택 프로그램", "PROGRAM", "2026-08-14", null, null, true
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startsAt").doesNotExist())
                .andExpect(jsonPath("$.data.collectParticipation").value(true))
                .andExpect(jsonPath("$.data.selectionCount").value(0))
                .andReturn();

        JsonNode createdData = objectMapper.readTree(created.getResponse().getContentAsString()).path("data");
        long scheduleId = createdData.path("id").asLong();
        long optionId = createdData.path("participationOptionId").asLong();

        createParticipant("Schedule Participant", "010-2345-6789");

        mockMvc.perform(get("/api/admin/schedules/" + scheduleId)
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participationOptionId").value(optionId))
                .andExpect(jsonPath("$.data.selectionCount").value(1));

        mockMvc.perform(patch("/api/admin/schedules/" + scheduleId)
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleRequestWithParticipation(
                                "선택 프로그램 수정", "PROGRAM", "2026-08-15", "14:00", "15:00", false
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.collectParticipation").value(false))
                .andExpect(jsonPath("$.data.participationOptionId").value(optionId))
                .andExpect(jsonPath("$.data.selectionCount").value(1));

        Map<String, Object> option = jdbcTemplate.queryForMap(
                "SELECT label, event_date, is_active FROM retreat_participation_options WHERE id = ?",
                optionId
        );
        assertThat(option.get("label")).isEqualTo("선택 프로그램 수정");
        assertThat(option.get("event_date").toString()).isEqualTo("2026-08-15");
        assertThat(option.get("is_active")).isEqualTo(false);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registration_participation_options WHERE option_id = ?",
                Integer.class,
                optionId
        )).isEqualTo(1);
    }

    @Test
    void retreatDateChangeMovesTimetableAndHidesItemsOutsideShortenedPeriod() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        Long secondDayScheduleId = createScheduleWithParticipation(
                chairToken, "둘째 날 일정", "PROGRAM", "2026-08-15"
        );
        Long thirdDayScheduleId = createScheduleWithParticipation(
                chairToken, "셋째 날 일정", "MEAL", "2026-08-16"
        );
        Long retreatId = jdbcTemplate.queryForObject(
                "SELECT id FROM retreats WHERE status = 'OPEN'",
                Long.class
        );

        mockMvc.perform(patch("/api/admin/retreats/" + retreatId)
                        .header("Authorization", "Bearer " + chairToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Moved Retreat",
                                "startsOn", "2026-09-01",
                                "endsOn", "2026-09-02"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startsOn").value("2026-09-01"))
                .andExpect(jsonPath("$.data.endsOn").value("2026-09-02"));

        mockMvc.perform(get("/api/admin/schedules/" + secondDayScheduleId)
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scheduleDate").value("2026-09-02"))
                .andExpect(jsonPath("$.data.active").value(true));

        mockMvc.perform(get("/api/admin/schedules/" + thirdDayScheduleId)
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scheduleDate").value("2026-09-03"))
                .andExpect(jsonPath("$.data.active").value(false));

        assertThat(jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM retreat_participation_options option
                        JOIN retreat_schedule_items schedule ON schedule.id = option.schedule_item_id
                        WHERE schedule.id = ?
                          AND option.event_date = DATE '2026-09-03'
                          AND option.is_active = FALSE
                        """,
                Integer.class,
                thirdDayScheduleId
        )).isEqualTo(1);
    }

    @Test
    void scheduleListFilteringByDateCategoryAndActiveWorks() throws Exception {
        String chairToken = accessTokenForRole(AdminRole.CHAIR);
        createSchedule(chairToken, "Visible Meal", "MEAL", "ALL", true, 0);
        createSchedule(chairToken, "Inactive Meal", "MEAL", "ALL", false, 1);
        createSchedule(chairToken, "Visible Prayer", "PRAYER", "ALL", true, 2);

        mockMvc.perform(get("/api/admin/schedules")
                        .param("date", "2026-08-14")
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
        Long groupId = createRetreatGroup(chairToken, "Check In Group 1");
        Long participantId = createParticipant("Filter Person", "010-5555-7890");
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
                        .param("keyword", "Young Adults")
                        .header("Authorization", "Bearer " + chairToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].participantId").value(participantId))
                .andExpect(jsonPath("$.data.content[0].retreatGroupId").value(groupId))
                .andExpect(jsonPath("$.data.content[0].middleGroupName").value("Dream"))
                .andExpect(jsonPath("$.data.content[0].cellName").value("Young Adults"));
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

    private Long createScheduleWithParticipation(
            String accessToken,
            String title,
            String category,
            String scheduleDate
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/schedules")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleRequestWithParticipation(
                                title, category, scheduleDate, "09:00", "10:00", true
                        )))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();
    }

    private Long createParticipant(String name, String phoneNumber) throws Exception {
        MvcResult result = createRegistration(name, phoneNumber, "Young Adults", true);
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("registration").path("id").asLong();
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
                "scheduleDate", "2026-08-14",
                "startsAt", "2026-08-14T09:00:00+09:00",
                "endsAt", "2026-08-14T10:00:00+09:00",
                "location", "Main Chapel",
                "category", category,
                "targetAudience", targetAudience,
                "active", active,
                "displayOrder", displayOrder,
                "collectParticipation", false
        ));
    }

    private String scheduleRequestWithParticipation(
            String title,
            String category,
            String scheduleDate,
            String startsTime,
            String endsTime,
            boolean collectParticipation
    ) throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("title", title);
        request.put("scheduleDate", scheduleDate);
        if (startsTime != null && endsTime != null) {
            request.put("startsAt", scheduleDate + "T" + startsTime + ":00+09:00");
            request.put("endsAt", scheduleDate + "T" + endsTime + ":00+09:00");
        }
        request.put("category", category);
        request.put("targetAudience", "ALL");
        request.put("active", true);
        request.put("displayOrder", startsTime == null ? 0 : 540);
        request.put("collectParticipation", collectParticipation);
        return objectMapper.writeValueAsString(request);
    }

    private String participationOptionRequest(
            String optionType,
            String label,
            String eventDate,
            int displayOrder,
            boolean active
    ) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "optionType", optionType,
                "label", label,
                "eventDate", eventDate,
                "displayOrder", displayOrder,
                "active", active
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
            String cellName,
            boolean privacyConsentAgreed
    ) throws Exception {
        return mockMvc.perform(post("/api/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest(name, phoneNumber, cellName, privacyConsentAgreed)))
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
            String cellName,
            boolean privacyConsentAgreed
    ) throws Exception {
        return objectMapper.writeValueAsString(Map.ofEntries(
                Map.entry("name", name),
                Map.entry("gender", "FEMALE"),
                Map.entry("birthYear", 1991),
                Map.entry("phoneNumber", phoneNumber),
                Map.entry("middleGroupName", "Dream"),
                Map.entry("cellName", cellName),
                Map.entry("privacyConsentAgreed", privacyConsentAgreed),
                Map.entry("lookupKey", DEFAULT_LOOKUP_KEY),
                Map.entry("attendanceType", "FULL"),
                Map.entry("selectedOptionIds", List.of()),
                Map.entry("inboundTransportationMethod", "GROUP_BUS"),
                Map.entry("outboundTransportationMethod", "GROUP_BUS")
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
        body.put("middleGroupName", "Dream");
        body.put("cellName", "Young Adults");
        body.put("privacyConsentAgreed", true);
        body.put("lookupKey", DEFAULT_LOOKUP_KEY);
        body.put("attendanceType", "FULL");
        body.put("selectedOptionIds", List.of());
        body.put("inboundTransportationMethod", "GROUP_BUS");
        body.put("outboundTransportationMethod", "GROUP_BUS");
        body.putAll(attendanceOverrides);
        if ("PARTIAL".equals(body.get("attendanceType"))) {
            body.putIfAbsent("plannedArrivalAt", "2026-08-01T19:00:00+09:00");
            body.putIfAbsent("plannedDepartureAt", "2026-08-02T21:00:00+09:00");
        }
        // Sync directional transportation if inbound/outbound overridden
        if (attendanceOverrides.containsKey("inboundTransportationMethod")) {
            // inbound already overridden
        } else {
            body.putIfAbsent("inboundTransportationMethod", "GROUP_BUS");
        }
        if (attendanceOverrides.containsKey("outboundTransportationMethod")) {
            // outbound already overridden
        } else {
            body.putIfAbsent("outboundTransportationMethod", "GROUP_BUS");
        }
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
        update.put("middleGroupName", "Updated Middle Group");
        update.put("cellName", "Updated Cell");
        update.put("attendanceType", "FULL");
        update.put("selectedOptionIds", List.of());
        update.put("inboundTransportationMethod", "GROUP_BUS");
        update.put("outboundTransportationMethod", "GROUP_BUS");
        update.putAll(attendanceOverrides);
        if ("PARTIAL".equals(update.get("attendanceType"))) {
            update.putIfAbsent("plannedArrivalAt", "2026-08-01T19:00:00+09:00");
            update.putIfAbsent("plannedDepartureAt", "2026-08-02T21:00:00+09:00");
        }
        // Ensure directional transportation fields are set
        if (!attendanceOverrides.containsKey("inboundTransportationMethod")) {
            update.putIfAbsent("inboundTransportationMethod", "GROUP_BUS");
        }
        if (!attendanceOverrides.containsKey("outboundTransportationMethod")) {
            update.putIfAbsent("outboundTransportationMethod", "GROUP_BUS");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Grace Kim");
        body.put("phoneLastFour", "5678");
        body.put("lookupKey", lookupKey);
        body.put("update", update);
        return objectMapper.writeValueAsString(body);
    }

    private Long participationOptionId(String eventDate, String label) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT id
                        FROM retreat_participation_options
                        WHERE event_date = CAST(? AS DATE) AND label = ?
                          AND retreat_id = (SELECT id FROM retreats WHERE status = 'OPEN')
                        """,
                Long.class,
                eventDate,
                label
        );
    }

    private boolean tableExists(String tableName) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.' || ?) IS NOT NULL",
                Boolean.class,
                tableName
        );
        return Boolean.TRUE.equals(exists);
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
