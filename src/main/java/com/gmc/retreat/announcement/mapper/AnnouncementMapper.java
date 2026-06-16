package com.gmc.retreat.announcement.mapper;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.announcement.domain.Announcement;
import com.gmc.retreat.announcement.domain.AnnouncementTarget;
import com.gmc.retreat.announcement.domain.AnnouncementTargetType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AnnouncementMapper {

    String ANNOUNCEMENT_COLUMNS = """
            a.id, a.title, a.content, a.is_pinned, a.is_active, a.visible_from, a.visible_until,
            a.created_by_admin_id, created_admin.email AS created_by_admin_email,
            created_admin.name AS created_by_admin_name, created_admin.role AS created_by_admin_role,
            a.updated_by_admin_id, updated_admin.email AS updated_by_admin_email,
            updated_admin.name AS updated_by_admin_name, updated_admin.role AS updated_by_admin_role,
            a.created_at, a.updated_at
            """;

    String ANNOUNCEMENT_JOINS = """
            FROM announcements a
            JOIN admin_users created_admin ON created_admin.id = a.created_by_admin_id
            JOIN admin_users updated_admin ON updated_admin.id = a.updated_by_admin_id
            """;

    @Select("""
            SELECT
            """ + ANNOUNCEMENT_COLUMNS + ANNOUNCEMENT_JOINS + """
            ORDER BY a.is_pinned DESC, a.created_at DESC, a.id DESC
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "title", javaType = String.class),
            @Arg(column = "content", javaType = String.class),
            @Arg(column = "is_pinned", javaType = Boolean.class),
            @Arg(column = "is_active", javaType = Boolean.class),
            @Arg(column = "visible_from", javaType = OffsetDateTime.class),
            @Arg(column = "visible_until", javaType = OffsetDateTime.class),
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
    List<Announcement> findAnnouncements();

    @Select("""
            SELECT
            """ + ANNOUNCEMENT_COLUMNS + ANNOUNCEMENT_JOINS + """
            WHERE a.id = #{id}
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "title", javaType = String.class),
            @Arg(column = "content", javaType = String.class),
            @Arg(column = "is_pinned", javaType = Boolean.class),
            @Arg(column = "is_active", javaType = Boolean.class),
            @Arg(column = "visible_from", javaType = OffsetDateTime.class),
            @Arg(column = "visible_until", javaType = OffsetDateTime.class),
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
    Optional<Announcement> findAnnouncementById(@Param("id") Long id);

    @Select("""
            SELECT id, announcement_id, target_type, target_value, created_at
            FROM announcement_targets
            WHERE announcement_id = #{announcementId}
            ORDER BY id ASC
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "announcement_id", javaType = Long.class),
            @Arg(column = "target_type", javaType = AnnouncementTargetType.class),
            @Arg(column = "target_value", javaType = String.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class)
    })
    List<AnnouncementTarget> findTargetsByAnnouncementId(@Param("announcementId") Long announcementId);

    @Insert("""
            INSERT INTO announcements (
                title, content, is_pinned, is_active, visible_from, visible_until,
                created_by_admin_id, updated_by_admin_id
            )
            VALUES (
                #{title}, #{content}, #{pinned}, #{active}, #{visibleFrom}, #{visibleUntil},
                #{adminId}, #{adminId}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAnnouncement(AnnouncementUpsert announcement);

    @Update("""
            UPDATE announcements
            SET title = #{title},
                content = #{content},
                is_pinned = #{pinned},
                is_active = #{active},
                visible_from = #{visibleFrom},
                visible_until = #{visibleUntil},
                updated_by_admin_id = #{adminId},
                updated_at = now()
            WHERE id = #{id}
            """)
    int updateAnnouncement(AnnouncementUpsert announcement);

    @Update("""
            UPDATE announcements
            SET is_active = #{active},
                updated_by_admin_id = #{adminId},
                updated_at = now()
            WHERE id = #{id}
            """)
    int updateActive(@Param("id") Long id, @Param("active") Boolean active, @Param("adminId") Long adminId);

    @Update("""
            UPDATE announcements
            SET is_pinned = #{pinned},
                updated_by_admin_id = #{adminId},
                updated_at = now()
            WHERE id = #{id}
            """)
    int updatePinned(@Param("id") Long id, @Param("pinned") Boolean pinned, @Param("adminId") Long adminId);

    @Insert("""
            INSERT INTO announcement_targets (announcement_id, target_type, target_value)
            VALUES (#{announcementId}, #{targetType}, #{targetValue})
            """)
    int insertTarget(AnnouncementTargetInsert target);

    @Delete("""
            DELETE FROM announcement_targets
            WHERE announcement_id = #{announcementId}
            """)
    int deleteTargetsByAnnouncementId(@Param("announcementId") Long announcementId);

    class AnnouncementUpsert {
        private Long id;
        private final String title;
        private final String content;
        private final Boolean pinned;
        private final Boolean active;
        private final OffsetDateTime visibleFrom;
        private final OffsetDateTime visibleUntil;
        private final Long adminId;

        public AnnouncementUpsert(
                Long id,
                String title,
                String content,
                Boolean pinned,
                Boolean active,
                OffsetDateTime visibleFrom,
                OffsetDateTime visibleUntil,
                Long adminId
        ) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.pinned = pinned;
            this.active = active;
            this.visibleFrom = visibleFrom;
            this.visibleUntil = visibleUntil;
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

        public String getContent() {
            return content;
        }

        public Boolean getPinned() {
            return pinned;
        }

        public Boolean getActive() {
            return active;
        }

        public OffsetDateTime getVisibleFrom() {
            return visibleFrom;
        }

        public OffsetDateTime getVisibleUntil() {
            return visibleUntil;
        }

        public Long getAdminId() {
            return adminId;
        }
    }

    record AnnouncementTargetInsert(
            Long announcementId,
            AnnouncementTargetType targetType,
            String targetValue
    ) {
    }
}
