package com.gmc.retreat.admin.mapper;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.admin.domain.AdminStatus;
import com.gmc.retreat.admin.domain.AdminUser;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminUserMapper {

    Optional<AdminUser> findById(@Param("id") Long id);

    Optional<AdminUser> findByEmail(@Param("email") String email);

    List<AdminUser> findAll();

    int insert(AdminUserInsert adminUser);

    int update(@Param("id") Long id, @Param("name") String name, @Param("role") AdminRole role);

    int updateStatus(@Param("id") Long id, @Param("status") AdminStatus status);

    int updatePasswordHash(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    int updateLastLoginAt(@Param("id") Long id);

    String findUiPreferences(@Param("id") Long id);

    int updateUiPreferences(@Param("id") Long id, @Param("json") String json);
}
