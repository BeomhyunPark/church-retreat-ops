package com.gmc.retreat.checkin.mapper;

import com.gmc.retreat.checkin.domain.CheckInEventAction;
import com.gmc.retreat.checkin.domain.CheckInMethod;
import com.gmc.retreat.checkin.domain.CheckInRosterItem;
import com.gmc.retreat.registration.domain.Gender;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CheckInMapper {

    String ROSTER_COLUMNS = """
            r.id AS participant_id, r.name, r.gender, r.birth_year, r.phone_last_four,
            r.church_cell_id, cc.name AS church_cell_name,
            mg.id AS middle_group_id, mg.name AS middle_group_name,
            rg.id AS retreat_group_id, rg.name AS retreat_group_name, rgm.leader AS retreat_group_leader,
            COALESCE(ci.checked_in, FALSE) AS checked_in, ci.checked_in_at, ci.check_in_method,
            ci.checked_in_by_admin_id, checked_admin.name AS checked_in_by_admin_name,
            ci.cancelled_at, ci.cancelled_by_admin_id, cancelled_admin.name AS cancelled_by_admin_name
            """;

    String ROSTER_JOINS = """
            FROM registrations r
            LEFT JOIN church_cells cc ON cc.id = r.church_cell_id
            LEFT JOIN church_middle_groups mg ON mg.id = cc.church_middle_group_id
            LEFT JOIN retreat_group_members rgm ON rgm.registration_id = r.id
            LEFT JOIN retreat_groups rg ON rg.id = rgm.retreat_group_id
            LEFT JOIN retreat_check_ins ci ON ci.participant_id = r.id
            LEFT JOIN admin_users checked_admin ON checked_admin.id = ci.checked_in_by_admin_id
            LEFT JOIN admin_users cancelled_admin ON cancelled_admin.id = ci.cancelled_by_admin_id
            """;

    @Select("""
            <script>
            SELECT
            """ + ROSTER_COLUMNS + ROSTER_JOINS + """
            <where>
                <if test="checkedIn != null">
                    COALESCE(ci.checked_in, FALSE) = #{checkedIn}
                </if>
                <if test="retreatGroupId != null">
                    AND rg.id = #{retreatGroupId}
                </if>
                <if test="churchCellId != null">
                    AND r.church_cell_id = #{churchCellId}
                </if>
                <if test="keyword != null and keyword != ''">
                    AND (
                        lower(r.name) LIKE lower(concat('%', #{keyword}, '%'))
                        OR r.phone_last_four = #{keyword}
                    )
                </if>
            </where>
            ORDER BY COALESCE(ci.checked_in, FALSE) ASC, rg.display_order ASC NULLS LAST,
                     rg.name ASC NULLS LAST, r.name ASC, r.id ASC
            LIMIT #{limit}
            OFFSET #{offset}
            </script>
            """)
    @ConstructorArgs({
            @Arg(column = "participant_id", javaType = Long.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "gender", javaType = Gender.class),
            @Arg(column = "birth_year", javaType = Integer.class),
            @Arg(column = "phone_last_four", javaType = String.class),
            @Arg(column = "church_cell_id", javaType = Long.class),
            @Arg(column = "church_cell_name", javaType = String.class),
            @Arg(column = "middle_group_id", javaType = Long.class),
            @Arg(column = "middle_group_name", javaType = String.class),
            @Arg(column = "retreat_group_id", javaType = Long.class),
            @Arg(column = "retreat_group_name", javaType = String.class),
            @Arg(column = "retreat_group_leader", javaType = Boolean.class),
            @Arg(column = "checked_in", javaType = Boolean.class),
            @Arg(column = "checked_in_at", javaType = OffsetDateTime.class),
            @Arg(column = "check_in_method", javaType = CheckInMethod.class),
            @Arg(column = "checked_in_by_admin_id", javaType = Long.class),
            @Arg(column = "checked_in_by_admin_name", javaType = String.class),
            @Arg(column = "cancelled_at", javaType = OffsetDateTime.class),
            @Arg(column = "cancelled_by_admin_id", javaType = Long.class),
            @Arg(column = "cancelled_by_admin_name", javaType = String.class)
    })
    List<CheckInRosterItem> findRoster(
            @Param("checkedIn") Boolean checkedIn,
            @Param("retreatGroupId") Long retreatGroupId,
            @Param("churchCellId") Long churchCellId,
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            """ + ROSTER_JOINS + """
            <where>
                <if test="checkedIn != null">
                    COALESCE(ci.checked_in, FALSE) = #{checkedIn}
                </if>
                <if test="retreatGroupId != null">
                    AND rg.id = #{retreatGroupId}
                </if>
                <if test="churchCellId != null">
                    AND r.church_cell_id = #{churchCellId}
                </if>
                <if test="keyword != null and keyword != ''">
                    AND (
                        lower(r.name) LIKE lower(concat('%', #{keyword}, '%'))
                        OR r.phone_last_four = #{keyword}
                    )
                </if>
            </where>
            </script>
            """)
    long countRoster(
            @Param("checkedIn") Boolean checkedIn,
            @Param("retreatGroupId") Long retreatGroupId,
            @Param("churchCellId") Long churchCellId,
            @Param("keyword") String keyword
    );

    @Select("""
            SELECT
            """ + ROSTER_COLUMNS + ROSTER_JOINS + """
            WHERE r.id = #{participantId}
            """)
    @ConstructorArgs({
            @Arg(column = "participant_id", javaType = Long.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "gender", javaType = Gender.class),
            @Arg(column = "birth_year", javaType = Integer.class),
            @Arg(column = "phone_last_four", javaType = String.class),
            @Arg(column = "church_cell_id", javaType = Long.class),
            @Arg(column = "church_cell_name", javaType = String.class),
            @Arg(column = "middle_group_id", javaType = Long.class),
            @Arg(column = "middle_group_name", javaType = String.class),
            @Arg(column = "retreat_group_id", javaType = Long.class),
            @Arg(column = "retreat_group_name", javaType = String.class),
            @Arg(column = "retreat_group_leader", javaType = Boolean.class),
            @Arg(column = "checked_in", javaType = Boolean.class),
            @Arg(column = "checked_in_at", javaType = OffsetDateTime.class),
            @Arg(column = "check_in_method", javaType = CheckInMethod.class),
            @Arg(column = "checked_in_by_admin_id", javaType = Long.class),
            @Arg(column = "checked_in_by_admin_name", javaType = String.class),
            @Arg(column = "cancelled_at", javaType = OffsetDateTime.class),
            @Arg(column = "cancelled_by_admin_id", javaType = Long.class),
            @Arg(column = "cancelled_by_admin_name", javaType = String.class)
    })
    Optional<CheckInRosterItem> findRosterItemByParticipantId(@Param("participantId") Long participantId);

    @Select("""
            SELECT COUNT(*)
            FROM registrations
            WHERE id = #{participantId}
            """)
    int countParticipantById(@Param("participantId") Long participantId);

    @Select("""
            INSERT INTO retreat_check_ins (
                participant_id, checked_in, checked_in_at, checked_in_by_admin_id, check_in_method
            )
            VALUES (
                #{participantId}, TRUE, now(), #{adminId}, #{method}
            )
            ON CONFLICT (participant_id) DO UPDATE
            SET checked_in = TRUE,
                checked_in_at = now(),
                checked_in_by_admin_id = EXCLUDED.checked_in_by_admin_id,
                check_in_method = EXCLUDED.check_in_method,
                cancelled_at = NULL,
                cancelled_by_admin_id = NULL,
                cancellation_reason = NULL,
                updated_at = now()
            WHERE retreat_check_ins.checked_in = FALSE
            RETURNING id
            """)
    Optional<Long> upsertCheckInIfNotCheckedIn(@Param("participantId") Long participantId,
                                               @Param("adminId") Long adminId,
                                               @Param("method") CheckInMethod method);

    @Select("""
            UPDATE retreat_check_ins
            SET checked_in = FALSE,
                cancelled_at = now(),
                cancelled_by_admin_id = #{adminId},
                cancellation_reason = #{reason},
                updated_at = now()
            WHERE participant_id = #{participantId}
              AND checked_in = TRUE
            RETURNING check_in_method
            """)
    Optional<CheckInMethod> cancelCheckInIfCheckedIn(@Param("participantId") Long participantId,
                                                     @Param("adminId") Long adminId,
                                                     @Param("reason") String reason);

    @Insert("""
            INSERT INTO retreat_check_in_events (
                participant_id, action, method, performed_by_admin_id, reason
            )
            VALUES (
                #{participantId}, #{action}, #{method}, #{adminId}, #{reason}
            )
            """)
    int insertEvent(CheckInEventInsert event);

    @Insert("""
            INSERT INTO participant_check_in_tokens (
                participant_id, token_hash, expires_at, issued_by_admin_id
            )
            VALUES (
                #{participantId}, #{tokenHash}, #{expiresAt}, #{adminId}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertToken(CheckInTokenInsert token);

    @Update("""
            UPDATE participant_check_in_tokens
            SET revoked_at = now()
            WHERE participant_id = #{participantId}
              AND revoked_at IS NULL
              AND expires_at > now()
            """)
    int revokeActiveTokensByParticipantId(@Param("participantId") Long participantId);

    @Select("""
            SELECT revoked_at
            FROM participant_check_in_tokens
            WHERE participant_id = #{participantId}
              AND revoked_at IS NOT NULL
            ORDER BY revoked_at DESC
            LIMIT 1
            """)
    Optional<OffsetDateTime> findLatestRevokedAtByParticipantId(@Param("participantId") Long participantId);

    class CheckInEventInsert {
        private final Long participantId;
        private final CheckInEventAction action;
        private final CheckInMethod method;
        private final Long adminId;
        private final String reason;

        public CheckInEventInsert(
                Long participantId,
                CheckInEventAction action,
                CheckInMethod method,
                Long adminId,
                String reason
        ) {
            this.participantId = participantId;
            this.action = action;
            this.method = method;
            this.adminId = adminId;
            this.reason = reason;
        }

        public Long getParticipantId() {
            return participantId;
        }

        public CheckInEventAction getAction() {
            return action;
        }

        public CheckInMethod getMethod() {
            return method;
        }

        public Long getAdminId() {
            return adminId;
        }

        public String getReason() {
            return reason;
        }
    }

    class CheckInTokenInsert {
        private Long id;
        private final Long participantId;
        private final String tokenHash;
        private final OffsetDateTime expiresAt;
        private final Long adminId;

        public CheckInTokenInsert(Long participantId, String tokenHash, OffsetDateTime expiresAt, Long adminId) {
            this.participantId = participantId;
            this.tokenHash = tokenHash;
            this.expiresAt = expiresAt;
            this.adminId = adminId;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getParticipantId() {
            return participantId;
        }

        public String getTokenHash() {
            return tokenHash;
        }

        public OffsetDateTime getExpiresAt() {
            return expiresAt;
        }

        public Long getAdminId() {
            return adminId;
        }
    }
}
