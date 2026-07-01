package com.gmc.retreat.checkin.mapper;

import com.gmc.retreat.checkin.domain.CheckInMethod;
import com.gmc.retreat.checkin.domain.CheckInRosterItem;
import com.gmc.retreat.checkin.domain.CheckInTokenRecord;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CheckInMapper {

    List<CheckInRosterItem> findRoster(
            @Param("checkedIn") Boolean checkedIn,
            @Param("retreatGroupId") Long retreatGroupId,
            @Param("churchCellId") Long churchCellId,
            @Param("keyword") String keyword,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long countRoster(
            @Param("checkedIn") Boolean checkedIn,
            @Param("retreatGroupId") Long retreatGroupId,
            @Param("churchCellId") Long churchCellId,
            @Param("keyword") String keyword
    );

    Optional<CheckInRosterItem> findRosterItemByParticipantId(@Param("participantId") Long participantId);

    int countParticipantById(@Param("participantId") Long participantId);

    Optional<Long> upsertCheckInIfNotCheckedIn(@Param("participantId") Long participantId,
                                               @Param("adminId") Long adminId,
                                               @Param("method") CheckInMethod method);

    Optional<CheckInMethod> cancelCheckInIfCheckedIn(@Param("participantId") Long participantId,
                                                     @Param("adminId") Long adminId,
                                                     @Param("reason") String reason);

    int insertEvent(CheckInEventInsert event);

    int insertToken(CheckInTokenInsert token);

    Optional<CheckInTokenRecord> findTokenByHash(@Param("tokenHash") String tokenHash);

    int revokeActiveTokensByParticipantId(@Param("participantId") Long participantId);

    Optional<OffsetDateTime> findLatestRevokedAtByParticipantId(@Param("participantId") Long participantId);
}
