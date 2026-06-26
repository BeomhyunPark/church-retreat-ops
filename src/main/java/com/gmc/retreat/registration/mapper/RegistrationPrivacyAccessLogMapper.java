package com.gmc.retreat.registration.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RegistrationPrivacyAccessLogMapper {

    int insert(RegistrationPrivacyAccessLogInsert accessLog);
}
