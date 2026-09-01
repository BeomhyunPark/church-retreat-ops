package com.gmc.retreat.fee.mapper;

import com.gmc.retreat.fee.domain.FeeEvent;
import com.gmc.retreat.fee.domain.FeeRosterItem;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FeeMapper {

    List<FeeRosterItem> findRoster(
            @Param("feePaid") Boolean feePaid,
            @Param("retreatGroupId") Long retreatGroupId,
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long countRoster(
            @Param("feePaid") Boolean feePaid,
            @Param("retreatGroupId") Long retreatGroupId,
            @Param("keyword") String keyword
    );

    Optional<FeeRosterItem> findRosterItemByParticipantId(@Param("participantId") Long participantId);

    int countParticipantById(@Param("participantId") Long participantId);

    Optional<Boolean> findFeePaidByParticipantId(@Param("participantId") Long participantId);

    Optional<Boolean> updateFeePaidIfChanged(@Param("participantId") Long participantId,
                                             @Param("feePaid") Boolean feePaid,
                                             @Param("adminId") Long adminId);

    int insertEvent(FeeEventInsert event);

    List<FeeEvent> findEventsByParticipantId(@Param("participantId") Long participantId, @Param("limit") int limit);
}
