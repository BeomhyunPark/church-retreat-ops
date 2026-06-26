package com.gmc.retreat.registration.mapper;

import com.gmc.retreat.registration.domain.AttendanceType;
import com.gmc.retreat.registration.domain.Registration;
import com.gmc.retreat.registration.domain.RegistrationStatus;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RegistrationMapper {

    Optional<Registration> findById(@Param("id") Long id);

    Optional<Registration> findActiveByNormalizedNameAndPhoneNumber(
            @Param("normalizedName") String normalizedName,
            @Param("phoneNumber") String phoneNumber
    );

    List<Registration> findActiveByNormalizedNameAndPhoneLastFour(
            @Param("normalizedName") String normalizedName,
            @Param("phoneLastFour") String phoneLastFour
    );

    List<Registration> findActiveByNormalizedName(@Param("normalizedName") String normalizedName);

    List<Registration> findPage(
            @Param("keyword") String keyword,
            @Param("status") RegistrationStatus status,
            @Param("feePaid") Boolean feePaid,
            @Param("newcomer") Boolean newcomer,
            @Param("careTarget") Boolean careTarget,
            @Param("checkedIn") Boolean checkedIn,
            @Param("retreatGroupAssigned") Boolean retreatGroupAssigned,
            @Param("churchCellAssigned") Boolean churchCellAssigned,
            @Param("attendanceType") AttendanceType attendanceType,
            @Param("transportationNeed") String transportationNeed,
            @Param("orderBy") String orderBy,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long countAll(
            @Param("keyword") String keyword,
            @Param("status") RegistrationStatus status,
            @Param("feePaid") Boolean feePaid,
            @Param("newcomer") Boolean newcomer,
            @Param("careTarget") Boolean careTarget,
            @Param("checkedIn") Boolean checkedIn,
            @Param("retreatGroupAssigned") Boolean retreatGroupAssigned,
            @Param("churchCellAssigned") Boolean churchCellAssigned,
            @Param("attendanceType") AttendanceType attendanceType,
            @Param("transportationNeed") String transportationNeed
    );

    int insert(RegistrationInsert registration);

    int overwrite(RegistrationOverwrite registration);

    int selfUpdate(RegistrationSelfUpdate registration);

    int updateFeePaid(@Param("id") Long id, @Param("feePaid") Boolean feePaid);

    int updateStatus(@Param("id") Long id, @Param("status") RegistrationStatus status);

    int updateManagement(RegistrationManagementUpdate registration);

    int updateChurchCell(@Param("id") Long id, @Param("churchCellId") Long churchCellId);
}
