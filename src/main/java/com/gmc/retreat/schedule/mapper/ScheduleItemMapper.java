package com.gmc.retreat.schedule.mapper;

import com.gmc.retreat.schedule.domain.ScheduleCategory;
import com.gmc.retreat.schedule.domain.ScheduleItem;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScheduleItemMapper {

    List<ScheduleItem> findScheduleItems(
            @Param("retreatId") Long retreatId,
            @Param("scheduleDate") LocalDate scheduleDate,
            @Param("category") ScheduleCategory category,
            @Param("active") Boolean active
    );

    Optional<ScheduleItem> findScheduleItemById(@Param("id") Long id);

    int insertScheduleItem(ScheduleItemUpsert scheduleItem);

    int updateScheduleItem(ScheduleItemUpsert scheduleItem);

    int updateActive(@Param("id") Long id, @Param("active") Boolean active, @Param("adminId") Long adminId);

    int shiftScheduleItems(
            @Param("retreatId") Long retreatId,
            @Param("dayShift") long dayShift,
            @Param("startsOn") LocalDate startsOn,
            @Param("endsOn") LocalDate endsOn,
            @Param("adminId") Long adminId
    );

    int syncLinkedParticipationOptions(@Param("retreatId") Long retreatId);
}
