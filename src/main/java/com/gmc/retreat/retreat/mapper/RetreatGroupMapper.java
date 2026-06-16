package com.gmc.retreat.retreat.mapper;

import com.gmc.retreat.registration.domain.Gender;
import com.gmc.retreat.retreat.domain.RetreatGroup;
import com.gmc.retreat.retreat.domain.RetreatGroupMember;
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
public interface RetreatGroupMapper {

    @Select("""
            SELECT id, name, description, display_order, active, created_at, updated_at
            FROM retreat_groups
            ORDER BY display_order ASC, name ASC, id ASC
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "display_order", javaType = Integer.class),
            @Arg(column = "active", javaType = Boolean.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    List<RetreatGroup> findGroups();

    @Select("""
            SELECT id, name, description, display_order, active, created_at, updated_at
            FROM retreat_groups
            WHERE id = #{id}
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "display_order", javaType = Integer.class),
            @Arg(column = "active", javaType = Boolean.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    Optional<RetreatGroup> findGroupById(@Param("id") Long id);

    @Select("""
            SELECT id, name, description, display_order, active, created_at, updated_at
            FROM retreat_groups
            WHERE name = #{name}
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "display_order", javaType = Integer.class),
            @Arg(column = "active", javaType = Boolean.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    Optional<RetreatGroup> findGroupByName(@Param("name") String name);

    @Insert("""
            INSERT INTO retreat_groups (name, description, display_order)
            VALUES (#{name}, #{description}, #{displayOrder})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertGroup(RetreatGroupUpsert group);

    @Update("""
            UPDATE retreat_groups
            SET name = #{name},
                description = #{description},
                display_order = #{displayOrder},
                updated_at = now()
            WHERE id = #{id}
            """)
    int updateGroup(RetreatGroupUpsert group);

    @Update("""
            UPDATE retreat_groups
            SET active = #{active},
                updated_at = now()
            WHERE id = #{id}
            """)
    int updateGroupActive(@Param("id") Long id, @Param("active") Boolean active);

    @Select("""
            SELECT gm.id, gm.retreat_group_id, rg.name AS retreat_group_name,
                   r.id AS participant_id, r.name AS participant_name, r.gender, r.birth_year,
                   r.church_cell_department, r.church_cell_id, cc.name AS church_cell_name,
                   mg.id AS middle_group_id, mg.name AS middle_group_name,
                   gm.leader, gm.created_at, gm.updated_at
            FROM retreat_group_members gm
            JOIN retreat_groups rg ON rg.id = gm.retreat_group_id
            JOIN registrations r ON r.id = gm.registration_id
            LEFT JOIN church_cells cc ON cc.id = r.church_cell_id
            LEFT JOIN church_middle_groups mg ON mg.id = cc.church_middle_group_id
            WHERE gm.retreat_group_id = #{groupId}
            ORDER BY gm.leader DESC, r.name ASC, r.id ASC
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "retreat_group_id", javaType = Long.class),
            @Arg(column = "retreat_group_name", javaType = String.class),
            @Arg(column = "participant_id", javaType = Long.class),
            @Arg(column = "participant_name", javaType = String.class),
            @Arg(column = "gender", javaType = Gender.class),
            @Arg(column = "birth_year", javaType = Integer.class),
            @Arg(column = "church_cell_department", javaType = String.class),
            @Arg(column = "church_cell_id", javaType = Long.class),
            @Arg(column = "church_cell_name", javaType = String.class),
            @Arg(column = "middle_group_id", javaType = Long.class),
            @Arg(column = "middle_group_name", javaType = String.class),
            @Arg(column = "leader", javaType = Boolean.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    List<RetreatGroupMember> findMembersByGroupId(@Param("groupId") Long groupId);

    @Select("""
            SELECT gm.id, gm.retreat_group_id, rg.name AS retreat_group_name,
                   r.id AS participant_id, r.name AS participant_name, r.gender, r.birth_year,
                   r.church_cell_department, r.church_cell_id, cc.name AS church_cell_name,
                   mg.id AS middle_group_id, mg.name AS middle_group_name,
                   gm.leader, gm.created_at, gm.updated_at
            FROM retreat_group_members gm
            JOIN retreat_groups rg ON rg.id = gm.retreat_group_id
            JOIN registrations r ON r.id = gm.registration_id
            LEFT JOIN church_cells cc ON cc.id = r.church_cell_id
            LEFT JOIN church_middle_groups mg ON mg.id = cc.church_middle_group_id
            ORDER BY rg.display_order ASC, rg.name ASC, gm.leader DESC, r.name ASC, r.id ASC
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "retreat_group_id", javaType = Long.class),
            @Arg(column = "retreat_group_name", javaType = String.class),
            @Arg(column = "participant_id", javaType = Long.class),
            @Arg(column = "participant_name", javaType = String.class),
            @Arg(column = "gender", javaType = Gender.class),
            @Arg(column = "birth_year", javaType = Integer.class),
            @Arg(column = "church_cell_department", javaType = String.class),
            @Arg(column = "church_cell_id", javaType = Long.class),
            @Arg(column = "church_cell_name", javaType = String.class),
            @Arg(column = "middle_group_id", javaType = Long.class),
            @Arg(column = "middle_group_name", javaType = String.class),
            @Arg(column = "leader", javaType = Boolean.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    List<RetreatGroupMember> findMembers();

    @Select("""
            SELECT COUNT(*)
            FROM retreat_group_members
            WHERE registration_id = #{participantId}
            """)
    int countAssignmentsForParticipant(@Param("participantId") Long participantId);

    @Select("""
            SELECT retreat_group_id
            FROM retreat_group_members
            WHERE registration_id = #{participantId}
            """)
    Optional<Long> findGroupIdByParticipantId(@Param("participantId") Long participantId);

    @Insert("""
            INSERT INTO retreat_group_members (retreat_group_id, registration_id, leader)
            VALUES (#{groupId}, #{participantId}, #{leader})
            """)
    int insertMember(
            @Param("groupId") Long groupId,
            @Param("participantId") Long participantId,
            @Param("leader") Boolean leader
    );

    @Update("""
            UPDATE retreat_group_members
            SET leader = FALSE,
                updated_at = now()
            WHERE retreat_group_id = #{groupId}
              AND leader = TRUE
            """)
    int clearGroupLeader(@Param("groupId") Long groupId);

    @Update("""
            UPDATE retreat_group_members
            SET leader = TRUE,
                updated_at = now()
            WHERE retreat_group_id = #{groupId}
              AND registration_id = #{participantId}
            """)
    int markLeader(@Param("groupId") Long groupId, @Param("participantId") Long participantId);

    @Delete("""
            DELETE FROM retreat_group_members
            WHERE registration_id = #{participantId}
            """)
    int deleteMemberByParticipantId(@Param("participantId") Long participantId);

    class RetreatGroupUpsert {
        private Long id;
        private final String name;
        private final String description;
        private final Integer displayOrder;

        public RetreatGroupUpsert(Long id, String name, String description, Integer displayOrder) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.displayOrder = displayOrder;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Integer getDisplayOrder() {
            return displayOrder;
        }
    }
}
