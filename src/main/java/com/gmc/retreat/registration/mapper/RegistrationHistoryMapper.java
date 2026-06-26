package com.gmc.retreat.registration.mapper;

import com.gmc.retreat.registration.domain.RegistrationHistory;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RegistrationHistoryMapper {

    int insert(RegistrationHistoryInsert history);

    List<RegistrationHistory> findByRegistrationId(@Param("registrationId") Long registrationId);
}
