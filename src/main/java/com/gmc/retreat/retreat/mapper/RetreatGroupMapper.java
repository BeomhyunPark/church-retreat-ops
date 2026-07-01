package com.gmc.retreat.retreat.mapper;

import com.gmc.retreat.retreat.domain.RetreatGroup;
import com.gmc.retreat.retreat.domain.RetreatGroupMember;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RetreatGroupMapper {

    List<RetreatGroup> findGroups();

    Optional<RetreatGroup> findGroupById(@Param("id") Long id);

    Optional<RetreatGroup> findGroupByName(@Param("name") String name);

    int insertGroup(RetreatGroupUpsert group);

    int updateGroup(RetreatGroupUpsert group);

    int updateGroupActive(@Param("id") Long id, @Param("active") Boolean active);

    int deleteGroupById(@Param("id") Long id);

    List<RetreatGroupMember> findMembersByGroupId(@Param("groupId") Long groupId);

    List<RetreatGroupMember> findMembers();

    Optional<Long> findGroupIdByParticipantId(@Param("participantId") Long participantId);

    int insertMember(
            @Param("groupId") Long groupId,
            @Param("participantId") Long participantId,
            @Param("leader") Boolean leader
    );

    int updateMemberGroup(@Param("groupId") Long groupId, @Param("participantId") Long participantId);

    int clearGroupLeader(@Param("groupId") Long groupId);

    int markLeader(@Param("groupId") Long groupId, @Param("participantId") Long participantId);

    int moveMemberToTop(@Param("groupId") Long groupId, @Param("participantId") Long participantId);

    int updateMemberDisplayOrder(
            @Param("groupId") Long groupId,
            @Param("participantId") Long participantId,
            @Param("displayOrder") Integer displayOrder
    );

    int deleteMembersByGroupId(@Param("groupId") Long groupId);

    int deleteMemberByParticipantId(@Param("participantId") Long participantId);
}
