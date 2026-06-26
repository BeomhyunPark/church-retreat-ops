package com.gmc.retreat.community.mapper;

import com.gmc.retreat.community.domain.ChurchCell;
import com.gmc.retreat.community.domain.ChurchMiddleGroup;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommunityMapper {

    List<ChurchMiddleGroup> findMiddleGroups();

    Optional<ChurchMiddleGroup> findMiddleGroupById(@Param("id") Long id);

    Optional<ChurchMiddleGroup> findMiddleGroupByName(@Param("name") String name);

    int insertMiddleGroup(ChurchMiddleGroupUpsert middleGroup);

    int updateMiddleGroup(ChurchMiddleGroupUpsert middleGroup);

    int updateMiddleGroupActive(@Param("id") Long id, @Param("active") Boolean active);

    List<ChurchCell> findCells(@Param("middleGroupId") Long middleGroupId, @Param("active") Boolean active);

    Optional<ChurchCell> findCellById(@Param("id") Long id);

    Optional<ChurchCell> findCellByMiddleGroupAndName(
            @Param("middleGroupId") Long middleGroupId,
            @Param("name") String name
    );

    int insertCell(ChurchCellUpsert cell);

    int updateCell(ChurchCellUpsert cell);

    int updateCellActive(@Param("id") Long id, @Param("active") Boolean active);
}
