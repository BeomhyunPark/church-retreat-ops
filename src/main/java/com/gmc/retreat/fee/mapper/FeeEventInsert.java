package com.gmc.retreat.fee.mapper;

public class FeeEventInsert {
    private final Long participantId;
    private final Boolean previousFeePaid;
    private final Boolean newFeePaid;
    private final Long adminId;
    private final String reason;

    public FeeEventInsert(
            Long participantId,
            Boolean previousFeePaid,
            Boolean newFeePaid,
            Long adminId,
            String reason
    ) {
        this.participantId = participantId;
        this.previousFeePaid = previousFeePaid;
        this.newFeePaid = newFeePaid;
        this.adminId = adminId;
        this.reason = reason;
    }

    public Long getParticipantId() {
        return participantId;
    }

    public Boolean getPreviousFeePaid() {
        return previousFeePaid;
    }

    public Boolean getNewFeePaid() {
        return newFeePaid;
    }

    public Long getAdminId() {
        return adminId;
    }

    public String getReason() {
        return reason;
    }
}
