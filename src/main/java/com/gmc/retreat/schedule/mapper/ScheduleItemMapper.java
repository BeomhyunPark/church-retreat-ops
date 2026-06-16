package com.gmc.retreat.schedule.mapper;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.schedule.domain.ScheduleCategory;
import com.gmc.retreat.schedule.domain.ScheduleItem;
import com.gmc.retreat.schedule.domain.ScheduleTargetAudience;
import java.time.LocalDate;
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
public interface ScheduleItemMapper {

    String SCHEDULE_COLUMNS = """
            s.id, s.title, s.description, s.schedule_date, s.starts_at, s.ends_at, s.location,
            s.category, s.target_audience, s.is_active, s.display_order,
            s.created_by_admin_id, created_admin.email AS created_by_admin_email,
            created_admin.name AS created_by_admin_name, created_admin.role AS created_by_admin_role,
            s.updated_by_admin_id, updated_admin.email AS updated_by_admin_email,
            updated_admin.name AS updated_by_admin_name, updated_admin.role AS updated_by_admin_role,
            s.created_at, s.updated_at
            """;

    String SCHEDULE_JOINS = """
            FROM retreat_schedule_items s
            JOIN admin_users created_admin ON created_admin.id = s.created_by_admin_id
            JOIN admin_users updated_admin ON updated_admin.id = s.updated_by_admin_id
            """;

    @Select("""
            <script>
            SELECT
            """ + SCHEDULE_COLUMNS + SCHEDULE_JOINS + """
            <where>
                <if test="scheduleDate != null">
                    s.schedule_date = #{scheduleDate}
                </if>
                <if test="category != null">
                    AND s.category = #{category}
                </if>
                <if test="active != null">
                    AND s.is_active = #{active}
                </if>
            </where>
            ORDER BY s.schedule_date ASC, s.display_order ASC, s.starts_at ASC, s.id ASC
            </script>
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "title", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "schedule_date", javaType = LocalDate.class),
            @Arg(column = "starts_at", javaType = OffsetDateTime.class),
            @Arg(column = "ends_at", javaType = OffsetDateTime.class),
            @Arg(column = "location", javaType = String.class),
            @Arg(column = "category", javaType = ScheduleCategory.class),
            @Arg(column = "target_audience", javaType = ScheduleTargetAudience.class),
            @Arg(column = "is_active", javaType = Boolean.class),
            @Arg(column = "display_order", javaType = Integer.class),
            @Arg(column = "created_by_admin_id", javaType = Long.class),
            @Arg(column = "created_by_admin_email", javaType = String.class),
            @Arg(column = "created_by_admin_name", javaType = String.class),
            @Arg(column = "created_by_admin_role", javaType = AdminRole.class),
            @Arg(column = "updated_by_admin_id", javaType = Long.class),
            @Arg(column = "updated_by_admin_email", javaType = String.class),
            @Arg(column = "updated_by_admin_name", javaType = String.class),
            @Arg(column = "updated_by_admin_role", javaType = AdminRole.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    List<ScheduleItem> findScheduleItems(
            @Param("scheduleDate") LocalDate scheduleDate,
            @Param("category") ScheduleCategory category,
            @Param("active") Boolean active
    );

    @Select("""
            SELECT
            """ + SCHEDULE_COLUMNS + SCHEDULE_JOINS + """
            WHERE s.id = #{id}
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "title", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "schedule_date", javaType = LocalDate.class),
            @Arg(column = "starts_at", javaType = OffsetDateTime.class),
            @Arg(column = "ends_at", javaType = OffsetDateTime.class),
            @Arg(column = "location", javaType = String.class),
            @Arg(column = "category", javaType = ScheduleCategory.class),
            @Arg(column = "target_audience", javaType = ScheduleTargetAudience.class),
            @Arg(column = "is_active", javaType = Boolean.class),
            @Arg(column = "display_order", javaType = Integer.class),
            @Arg(column = "created_by_admin_id", javaType = Long.class),
            @Arg(column = "created_by_admin_email", javaType = String.class),
            @Arg(column = "created_by_admin_name", javaType = String.class),
            @Arg(column = "created_by_admin_role", javaType = AdminRole.class),
            @Arg(column = "updated_by_admin_id", javaType = Long.class),
            @Arg(column = "updated_by_admin_email", javaType = String.class),
            @Arg(column = "updated_by_admin_name", javaType = String.class),
            @Arg(column = "updated_by_admin_role", javaType = AdminRole.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    Optional<ScheduleItem> findScheduleItemById(@Param("id") Long id);

    @Insert("""
            INSERT INTO retreat_schedule_items (
                title, description, schedule_date, starts_at, ends_at, location,
                category, target_audience, is_active, display_order,
                created_by_admin_id, updated_by_admin_id
            )
            VALUES (
                #{title}, #{description}, #{scheduleDate}, #{startsAt}, #{endsAt}, #{location},
                #{category}, #{targetAudience}, #{active}, #{displayOrder},
                #{adminId}, #{adminId}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertScheduleItem(ScheduleItemUpsert scheduleItem);

    @Update("""
            UPDATE retreat_schedule_items
            SET title = #{title},
                description = #{description},
                schedule_date = #{scheduleDate},
                starts_at = #{startsAt},
                ends_at = #{endsAt},
                location = #{location},
                category = #{category},
                target_audience = #{targetAudience},
                is_active = #{active},
                display_order = #{displayOrder},
                updated_by_admin_id = #{adminId},
                updated_at = now()
            WHERE id = #{id}
            """)
    int updateScheduleItem(ScheduleItemUpsert scheduleItem);

    @Update("""
            UPDATE retreat_schedule_items
            SET is_active = #{active},
                updated_by_admin_id = #{adminId},
                updated_at = now()
            WHERE id = #{id}
            """)
    int updateActive(@Param("id") Long id, @Param("active") Boolean active, @Param("adminId") Long adminId);

    class ScheduleItemUpsert {
        private Long id;
        private final String title;
        private final String description;
        private final LocalDate scheduleDate;
        private final OffsetDateTime startsAt;
        private final OffsetDateTime endsAt;
        private final String location;
        private final ScheduleCategory category;
        private final ScheduleTargetAudience targetAudience;
        private final Boolean active;
        private final Integer displayOrder;
        private final Long adminId;

        public ScheduleItemUpsert(
                Long id,
                String title,
                String description,
                LocalDate scheduleDate,
                OffsetDateTime startsAt,
                OffsetDateTime endsAt,
                String location,
                ScheduleCategory category,
                ScheduleTargetAudience targetAudience,
                Boolean active,
                Integer displayOrder,
                Long adminId
        ) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.scheduleDate = scheduleDate;
            this.startsAt = startsAt;
            this.endsAt = endsAt;
            this.location = location;
            this.category = category;
            this.targetAudience = targetAudience;
            this.active = active;
            this.displayOrder = displayOrder;
            this.adminId = adminId;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public LocalDate getScheduleDate() {
            return scheduleDate;
        }

        public OffsetDateTime getStartsAt() {
            return startsAt;
        }

        public OffsetDateTime getEndsAt() {
            return endsAt;
        }

        public String getLocation() {
            return location;
        }

        public ScheduleCategory getCategory() {
            return category;
        }

        public ScheduleTargetAudience getTargetAudience() {
            return targetAudience;
        }

        public Boolean getActive() {
            return active;
        }

        public Integer getDisplayOrder() {
            return displayOrder;
        }

        public Long getAdminId() {
            return adminId;
        }
    }
}
