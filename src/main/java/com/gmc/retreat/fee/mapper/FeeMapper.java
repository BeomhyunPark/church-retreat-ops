package com.gmc.retreat.fee.mapper;

import com.gmc.retreat.fee.domain.FeeEvent;
import com.gmc.retreat.fee.domain.FeeRosterItem;
import com.gmc.retreat.registration.domain.Gender;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FeeMapper {

    String FEE_COLUMNS = """
            r.id AS participant_id, r.name, r.gender, r.birth_year, r.phone_last_four,
            r.church_cell_id, cc.name AS church_cell_name,
            mg.id AS middle_group_id, mg.name AS middle_group_name,
            rg.id AS retreat_group_id, rg.name AS retreat_group_name, rgm.leader AS retreat_group_leader,
            r.fee_paid, r.fee_status_updated_at, r.fee_status_updated_by_admin_id,
            fee_admin.name AS fee_status_updated_by_admin_name
            """;

    String FEE_JOINS = """
            FROM registrations r
            LEFT JOIN church_cells cc ON cc.id = r.church_cell_id
            LEFT JOIN church_middle_groups mg ON mg.id = cc.church_middle_group_id
            LEFT JOIN retreat_group_members rgm ON rgm.registration_id = r.id
            LEFT JOIN retreat_groups rg ON rg.id = rgm.retreat_group_id
            LEFT JOIN admin_users fee_admin ON fee_admin.id = r.fee_status_updated_by_admin_id
            """;

    @Select("""
            <script>
            SELECT
            """ + FEE_COLUMNS + FEE_JOINS + """
            <where>
                <if test="feePaid != null">
                    r.fee_paid = #{feePaid}
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
            ORDER BY r.fee_paid ASC, rg.display_order ASC NULLS LAST,
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
            @Arg(column = "fee_paid", javaType = Boolean.class),
            @Arg(column = "fee_status_updated_at", javaType = OffsetDateTime.class),
            @Arg(column = "fee_status_updated_by_admin_id", javaType = Long.class),
            @Arg(column = "fee_status_updated_by_admin_name", javaType = String.class)
    })
    List<FeeRosterItem> findRoster(
            @Param("feePaid") Boolean feePaid,
            @Param("retreatGroupId") Long retreatGroupId,
            @Param("churchCellId") Long churchCellId,
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            """ + FEE_JOINS + """
            <where>
                <if test="feePaid != null">
                    r.fee_paid = #{feePaid}
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
            @Param("feePaid") Boolean feePaid,
            @Param("retreatGroupId") Long retreatGroupId,
            @Param("churchCellId") Long churchCellId,
            @Param("keyword") String keyword
    );

    @Select("""
            SELECT
            """ + FEE_COLUMNS + FEE_JOINS + """
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
            @Arg(column = "fee_paid", javaType = Boolean.class),
            @Arg(column = "fee_status_updated_at", javaType = OffsetDateTime.class),
            @Arg(column = "fee_status_updated_by_admin_id", javaType = Long.class),
            @Arg(column = "fee_status_updated_by_admin_name", javaType = String.class)
    })
    Optional<FeeRosterItem> findRosterItemByParticipantId(@Param("participantId") Long participantId);

    @Select("""
            SELECT COUNT(*)
            FROM registrations
            WHERE id = #{participantId}
            """)
    int countParticipantById(@Param("participantId") Long participantId);

    @Select("""
            SELECT fee_paid
            FROM registrations
            WHERE id = #{participantId}
            """)
    Optional<Boolean> findFeePaidByParticipantId(@Param("participantId") Long participantId);

    @Select("""
            WITH target AS (
                SELECT id, fee_paid
                FROM registrations
                WHERE id = #{participantId}
                  AND fee_paid <> #{feePaid}
                FOR UPDATE
            ),
            updated AS (
                UPDATE registrations r
                SET fee_paid = #{feePaid},
                    fee_status_updated_at = now(),
                    fee_status_updated_by_admin_id = #{adminId},
                    updated_at = now()
                FROM target
                WHERE r.id = target.id
                RETURNING r.id
            )
            SELECT target.fee_paid
            FROM target
            JOIN updated ON updated.id = target.id
            """)
    Optional<Boolean> updateFeePaidIfChanged(@Param("participantId") Long participantId,
                                             @Param("feePaid") Boolean feePaid,
                                             @Param("adminId") Long adminId);

    @Insert("""
            INSERT INTO registration_fee_events (
                registration_id, previous_fee_paid, new_fee_paid, changed_by_admin_id, reason
            )
            VALUES (
                #{participantId}, #{previousFeePaid}, #{newFeePaid}, #{adminId}, #{reason}
            )
            """)
    int insertEvent(FeeEventInsert event);

    @Select("""
            SELECT e.id, e.registration_id AS participant_id,
                   e.previous_fee_paid, e.new_fee_paid,
                   e.changed_by_admin_id, a.name AS changed_by_admin_name,
                   e.reason, e.created_at
            FROM registration_fee_events e
            JOIN admin_users a ON a.id = e.changed_by_admin_id
            WHERE e.registration_id = #{participantId}
            ORDER BY e.created_at DESC, e.id DESC
            LIMIT #{limit}
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "participant_id", javaType = Long.class),
            @Arg(column = "previous_fee_paid", javaType = Boolean.class),
            @Arg(column = "new_fee_paid", javaType = Boolean.class),
            @Arg(column = "changed_by_admin_id", javaType = Long.class),
            @Arg(column = "changed_by_admin_name", javaType = String.class),
            @Arg(column = "reason", javaType = String.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class)
    })
    List<FeeEvent> findEventsByParticipantId(@Param("participantId") Long participantId, @Param("limit") int limit);

    class FeeEventInsert {
        private final Long participantId;
        private final Boolean previousFeePaid;
        private final Boolean newFeePaid;
        private final Long adminId;
        private final String reason;

        public FeeEventInsert(
                Long participantId,
                Boolean previousFeePaid,
                Boolean newFeePaid,
                Long adminId,
                String reason
        ) {
            this.participantId = participantId;
            this.previousFeePaid = previousFeePaid;
            this.newFeePaid = newFeePaid;
            this.adminId = adminId;
            this.reason = reason;
        }

        public Long getParticipantId() {
            return participantId;
        }

        public Boolean getPreviousFeePaid() {
            return previousFeePaid;
        }

        public Boolean getNewFeePaid() {
            return newFeePaid;
        }

        public Long getAdminId() {
            return adminId;
        }

        public String getReason() {
            return reason;
        }
    }
}
