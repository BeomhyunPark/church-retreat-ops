package com.gmc.retreat.retreat.mapper;

import com.gmc.retreat.retreat.domain.Retreat;
import com.gmc.retreat.retreat.domain.RetreatStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RetreatMapper {

    List<Retreat> findAll();

    Optional<Retreat> findById(@Param("id") Long id);

    Optional<Retreat> findCurrent();

    Optional<Retreat> findOpen();

    Optional<Retreat> findRegistrationOpen();

    int insert(RetreatInsert retreat);

    int updateMetadata(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("startsOn") LocalDate startsOn,
            @Param("endsOn") LocalDate endsOn
    );

    int updateStatus(
            @Param("id") Long id,
            @Param("status") RetreatStatus status,
            @Param("participantCount") Integer participantCount
    );

    int updateRegistrationOpen(
            @Param("id") Long id,
            @Param("registrationOpen") Boolean registrationOpen
    );

    int countRegisteredParticipants(@Param("retreatId") Long retreatId);
}
