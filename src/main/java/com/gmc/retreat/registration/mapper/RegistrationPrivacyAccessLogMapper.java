package com.gmc.retreat.registration.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RegistrationPrivacyAccessLogMapper {

    @Insert("""
            INSERT INTO registration_privacy_access_logs (
                registration_id, admin_user_id, access_type, sensitive_fields
            )
            VALUES (
                #{registrationId}, #{adminUserId}, #{accessType}, #{sensitiveFields}
            )
            """)
    int insert(RegistrationPrivacyAccessLogInsert accessLog);

    record RegistrationPrivacyAccessLogInsert(
            Long registrationId,
            Long adminUserId,
            String accessType,
            String sensitiveFields
    ) {
    }
}
