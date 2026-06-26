package com.gmc.retreat.announcement.mapper;

import com.gmc.retreat.announcement.domain.Announcement;
import com.gmc.retreat.announcement.domain.AnnouncementTarget;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnouncementMapper {

    List<Announcement> findAnnouncements();

    Optional<Announcement> findAnnouncementById(@Param("id") Long id);

    List<AnnouncementTarget> findTargetsByAnnouncementId(@Param("announcementId") Long announcementId);

    int insertAnnouncement(AnnouncementUpsert announcement);

    int updateAnnouncement(AnnouncementUpsert announcement);

    int updateActive(@Param("id") Long id, @Param("active") Boolean active, @Param("adminId") Long adminId);

    int updatePinned(@Param("id") Long id, @Param("pinned") Boolean pinned, @Param("adminId") Long adminId);

    int insertTarget(AnnouncementTargetInsert target);

    int deleteTargetsByAnnouncementId(@Param("announcementId") Long announcementId);
}
