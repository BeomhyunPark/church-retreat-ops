package com.gmc.retreat.registration.mapper;

import com.gmc.retreat.registration.domain.RegistrationActorType;
import com.gmc.retreat.registration.domain.RegistrationHistory;
import com.gmc.retreat.registration.domain.RegistrationHistoryChangeType;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RegistrationHistoryMapper {

    @Insert("""
            INSERT INTO registration_histories (
                registration_id, change_type, previous_snapshot_json, new_snapshot_json,
                actor_type, actor_admin_user_id
            )
            VALUES (
                #{registrationId}, #{changeType}, #{previousSnapshotJson}, #{newSnapshotJson},
                #{actorType}, #{actorAdminUserId}
            )
            """)
    int insert(RegistrationHistoryInsert history);

    @Select("""
            SELECT id, registration_id, change_type, previous_snapshot_json, new_snapshot_json,
                   actor_type, actor_admin_user_id, created_at
            FROM registration_histories
            WHERE registration_id = #{registrationId}
            ORDER BY created_at DESC, id DESC
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "registration_id", javaType = Long.class),
            @Arg(column = "change_type", javaType = RegistrationHistoryChangeType.class),
            @Arg(column = "previous_snapshot_json", javaType = String.class),
            @Arg(column = "new_snapshot_json", javaType = String.class),
            @Arg(column = "actor_type", javaType = RegistrationActorType.class),
            @Arg(column = "actor_admin_user_id", javaType = Long.class),
            @Arg(column = "created_at", javaType = OffsetDateTime.class)
    })
    List<RegistrationHistory> findByRegistrationId(@Param("registrationId") Long registrationId);

    record RegistrationHistoryInsert(
            Long registrationId,
            RegistrationHistoryChangeType changeType,
            String previousSnapshotJson,
            String newSnapshotJson,
            RegistrationActorType actorType,
            Long actorAdminUserId
    ) {
    }
}
