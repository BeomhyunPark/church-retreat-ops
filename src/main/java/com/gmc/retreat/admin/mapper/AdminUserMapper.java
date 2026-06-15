package com.gmc.retreat.admin.mapper;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.admin.domain.AdminStatus;
import com.gmc.retreat.admin.domain.AdminUser;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminUserMapper {

    @Select("""
            SELECT id, email, password_hash, name, role, status, last_login_at, created_at, updated_at
            FROM admin_users
            WHERE id = #{id}
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "email", javaType = String.class),
            @Arg(column = "password_hash", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "role", javaType = AdminRole.class),
            @Arg(column = "status", javaType = AdminStatus.class),
            @Arg(column = "last_login_at", javaType = OffsetDateTime.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    Optional<AdminUser> findById(@Param("id") Long id);

    @Select("""
            SELECT id, email, password_hash, name, role, status, last_login_at, created_at, updated_at
            FROM admin_users
            WHERE email = #{email}
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "email", javaType = String.class),
            @Arg(column = "password_hash", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "role", javaType = AdminRole.class),
            @Arg(column = "status", javaType = AdminStatus.class),
            @Arg(column = "last_login_at", javaType = OffsetDateTime.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class),
            @Arg(column = "updated_at", javaType = OffsetDateTime.class)
    })
    Optional<AdminUser> findByEmail(@Param("email") String email);

    @Insert("""
            INSERT INTO admin_users (email, password_hash, name, role, status)
            VALUES (#{email}, #{passwordHash}, #{name}, #{role}, #{status})
            """)
    int insert(AdminUserInsert adminUser);

    @Update("""
            UPDATE admin_users
            SET last_login_at = now(), updated_at = now()
            WHERE id = #{id}
            """)
    int updateLastLoginAt(@Param("id") Long id);

    record AdminUserInsert(
            String email,
            String passwordHash,
            String name,
            AdminRole role,
            AdminStatus status
    ) {
    }

}
