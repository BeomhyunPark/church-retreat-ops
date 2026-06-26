package com.gmc.retreat.announcement.mapper;

import java.time.OffsetDateTime;

public class AnnouncementUpsert {
    private Long id;
    private final String title;
    private final String content;
    private final Boolean pinned;
    private final Boolean active;
    private final OffsetDateTime visibleFrom;
    private final OffsetDateTime visibleUntil;
    private final Long adminId;

    public AnnouncementUpsert(
            Long id,
            String title,
            String content,
            Boolean pinned,
            Boolean active,
            OffsetDateTime visibleFrom,
            OffsetDateTime visibleUntil,
            Long adminId
    ) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.active = active;
        this.visibleFrom = visibleFrom;
        this.visibleUntil = visibleUntil;
        this.adminId = adminId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Boolean getPinned() {
        return pinned;
    }

    public Boolean getActive() {
        return active;
    }

    public OffsetDateTime getVisibleFrom() {
        return visibleFrom;
    }

    public OffsetDateTime getVisibleUntil() {
        return visibleUntil;
    }

    public Long getAdminId() {
        return adminId;
    }
}
