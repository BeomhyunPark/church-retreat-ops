package com.gmc.retreat.community.mapper;

import com.gmc.retreat.community.domain.ChurchCell;
import com.gmc.retreat.community.domain.ChurchMiddleGroup;
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
public interface CommunityMapper {

    @Select("""
            SELECT id, name, elder_name, description, display_order, active, created_at, updated_at
            FROM church_middle_groups
            ORDER BY display_order ASC, name ASC, id ASC
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "elder_name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "display_order", javaType = Integer.class),
            @Arg(column = "active", javaType = Boolean.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    List<ChurchMiddleGroup> findMiddleGroups();

    @Select("""
            SELECT id, name, elder_name, description, display_order, active, created_at, updated_at
            FROM church_middle_groups
            WHERE id = #{id}
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "elder_name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "display_order", javaType = Integer.class),
            @Arg(column = "active", javaType = Boolean.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    Optional<ChurchMiddleGroup> findMiddleGroupById(@Param("id") Long id);

    @Select("""
            SELECT id, name, elder_name, description, display_order, active, created_at, updated_at
            FROM church_middle_groups
            WHERE name = #{name}
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "elder_name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "display_order", javaType = Integer.class),
            @Arg(column = "active", javaType = Boolean.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    Optional<ChurchMiddleGroup> findMiddleGroupByName(@Param("name") String name);

    @Insert("""
            INSERT INTO church_middle_groups (name, elder_name, description, display_order)
            VALUES (#{name}, #{elderName}, #{description}, #{displayOrder})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMiddleGroup(ChurchMiddleGroupUpsert middleGroup);

    @Update("""
            UPDATE church_middle_groups
            SET name = #{name},
                elder_name = #{elderName},
                description = #{description},
                display_order = #{displayOrder},
                updated_at = now()
            WHERE id = #{id}
            """)
    int updateMiddleGroup(ChurchMiddleGroupUpsert middleGroup);

    @Update("""
            UPDATE church_middle_groups
            SET active = #{active},
                updated_at = now()
            WHERE id = #{id}
            """)
    int updateMiddleGroupActive(@Param("id") Long id, @Param("active") Boolean active);

    @Select("""
            SELECT c.id, c.church_middle_group_id, mg.name AS middle_group_name, c.name,
                   c.cell_leader_name, c.description, c.display_order, c.active,
                   c.created_at, c.updated_at
            FROM church_cells c
            JOIN church_middle_groups mg ON mg.id = c.church_middle_group_id
            WHERE (#{middleGroupId}::BIGINT IS NULL OR c.church_middle_group_id = #{middleGroupId})
              AND (#{active}::BOOLEAN IS NULL OR c.active = #{active})
            ORDER BY mg.display_order ASC, mg.name ASC, c.display_order ASC, c.name ASC, c.id ASC
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "church_middle_group_id", javaType = Long.class),
            @Arg(column = "middle_group_name", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "cell_leader_name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "display_order", javaType = Integer.class),
            @Arg(column = "active", javaType = Boolean.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    List<ChurchCell> findCells(@Param("middleGroupId") Long middleGroupId, @Param("active") Boolean active);

    @Select("""
            SELECT c.id, c.church_middle_group_id, mg.name AS middle_group_name, c.name,
                   c.cell_leader_name, c.description, c.display_order, c.active,
                   c.created_at, c.updated_at
            FROM church_cells c
            JOIN church_middle_groups mg ON mg.id = c.church_middle_group_id
            WHERE c.id = #{id}
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "church_middle_group_id", javaType = Long.class),
            @Arg(column = "middle_group_name", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "cell_leader_name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "display_order", javaType = Integer.class),
            @Arg(column = "active", javaType = Boolean.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    Optional<ChurchCell> findCellById(@Param("id") Long id);

    @Select("""
            SELECT c.id, c.church_middle_group_id, mg.name AS middle_group_name, c.name,
                   c.cell_leader_name, c.description, c.display_order, c.active,
                   c.created_at, c.updated_at
            FROM church_cells c
            JOIN church_middle_groups mg ON mg.id = c.church_middle_group_id
            WHERE c.church_middle_group_id = #{middleGroupId}
              AND c.name = #{name}
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "church_middle_group_id", javaType = Long.class),
            @Arg(column = "middle_group_name", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "cell_leader_name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "display_order", javaType = Integer.class),
            @Arg(column = "active", javaType = Boolean.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    Optional<ChurchCell> findCellByMiddleGroupAndName(
            @Param("middleGroupId") Long middleGroupId,
            @Param("name") String name
    );

    @Insert("""
            INSERT INTO church_cells (
                church_middle_group_id, name, cell_leader_name, description, display_order
            )
            VALUES (
                #{middleGroupId}, #{name}, #{cellLeaderName}, #{description}, #{displayOrder}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCell(ChurchCellUpsert cell);

    @Update("""
            UPDATE church_cells
            SET church_middle_group_id = #{middleGroupId},
                name = #{name},
                cell_leader_name = #{cellLeaderName},
                description = #{description},
                display_order = #{displayOrder},
                updated_at = now()
            WHERE id = #{id}
            """)
    int updateCell(ChurchCellUpsert cell);

    @Update("""
            UPDATE church_cells
            SET active = #{active},
                updated_at = now()
            WHERE id = #{id}
            """)
    int updateCellActive(@Param("id") Long id, @Param("active") Boolean active);

    class ChurchMiddleGroupUpsert {
        private Long id;
        private final String name;
        private final String elderName;
        private final String description;
        private final Integer displayOrder;

        public ChurchMiddleGroupUpsert(
                Long id,
                String name,
                String elderName,
                String description,
                Integer displayOrder
        ) {
            this.id = id;
            this.name = name;
            this.elderName = elderName;
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

        public String getElderName() {
            return elderName;
        }

        public String getDescription() {
            return description;
        }

        public Integer getDisplayOrder() {
            return displayOrder;
        }
    }

    class ChurchCellUpsert {
        private Long id;
        private final Long middleGroupId;
        private final String name;
        private final String cellLeaderName;
        private final String description;
        private final Integer displayOrder;

        public ChurchCellUpsert(
                Long id,
                Long middleGroupId,
                String name,
                String cellLeaderName,
                String description,
                Integer displayOrder
        ) {
            this.id = id;
            this.middleGroupId = middleGroupId;
            this.name = name;
            this.cellLeaderName = cellLeaderName;
            this.description = description;
            this.displayOrder = displayOrder;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getMiddleGroupId() {
            return middleGroupId;
        }

        public String getName() {
            return name;
        }

        public String getCellLeaderName() {
            return cellLeaderName;
        }

        public String getDescription() {
            return description;
        }

        public Integer getDisplayOrder() {
            return displayOrder;
        }
    }
}
