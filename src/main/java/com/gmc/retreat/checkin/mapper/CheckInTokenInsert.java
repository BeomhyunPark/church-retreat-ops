package com.gmc.retreat.checkin.mapper;

import java.time.OffsetDateTime;

public class CheckInTokenInsert {
    private Long id;
    private final Long participantId;
    private final String tokenHash;
    private final OffsetDateTime expiresAt;
    private final Long adminId;

    public CheckInTokenInsert(Long participantId, String tokenHash, OffsetDateTime expiresAt, Long adminId) {
        this.participantId = participantId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.adminId = adminId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParticipantId() {
        return participantId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public Long getAdminId() {
        return adminId;
    }
}
