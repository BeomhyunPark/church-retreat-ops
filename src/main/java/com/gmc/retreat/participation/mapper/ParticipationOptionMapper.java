package com.gmc.retreat.participation.mapper;

import com.gmc.retreat.participation.domain.ParticipationOption;
import com.gmc.retreat.participation.domain.RegistrationParticipationSelection;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ParticipationOptionMapper {

    List<ParticipationOption> findCurrentOptions();

    List<ParticipationOption> findActiveOpenOptions();

    Optional<ParticipationOption> findCurrentOptionById(@Param("id") Long id);

    Optional<ParticipationOption> findCurrentOptionByScheduleItemId(@Param("scheduleItemId") Long scheduleItemId);

    Optional<Long> findCurrentOptionIdByDateAndLabel(
            @Param("eventDate") LocalDate eventDate,
            @Param("label") String label
    );

    int insert(ParticipationOptionUpsert option);

    int update(ParticipationOptionUpsert option);

    int updateActive(@Param("id") Long id, @Param("active") Boolean active);

    int shiftUnlinkedOptions(
            @Param("retreatId") Long retreatId,
            @Param("dayShift") long dayShift,
            @Param("startsOn") LocalDate startsOn,
            @Param("endsOn") LocalDate endsOn
    );

    List<Long> findActiveOptionIds(@Param("retreatId") Long retreatId);

    List<Long> findValidActiveOptionIds(
            @Param("retreatId") Long retreatId,
            @Param("optionIds") List<Long> optionIds
    );

    List<Long> findSelectedOptionIds(@Param("registrationId") Long registrationId);

    List<RegistrationParticipationSelection> findSelectionsByRegistrationIds(
            @Param("registrationIds") List<Long> registrationIds
    );

    int deleteSelections(@Param("registrationId") Long registrationId);

    int insertSelections(
            @Param("registrationId") Long registrationId,
            @Param("optionIds") List<Long> optionIds
    );
}
