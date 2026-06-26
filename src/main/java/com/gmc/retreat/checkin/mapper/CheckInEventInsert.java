package com.gmc.retreat.checkin.mapper;

import com.gmc.retreat.checkin.domain.CheckInEventAction;
import com.gmc.retreat.checkin.domain.CheckInMethod;

public class CheckInEventInsert {
    private final Long participantId;
    private final CheckInEventAction action;
    private final CheckInMethod method;
    private final Long adminId;
    private final String reason;

    public CheckInEventInsert(
            Long participantId,
            CheckInEventAction action,
            CheckInMethod method,
            Long adminId,
            String reason
    ) {
        this.participantId = participantId;
        this.action = action;
        this.method = method;
        this.adminId = adminId;
        this.reason = reason;
    }

    public Long getParticipantId() {
        return participantId;
    }

    public CheckInEventAction getAction() {
        return action;
    }

    public CheckInMethod getMethod() {
        return method;
    }

    public Long getAdminId() {
        return adminId;
    }

    public String getReason() {
        return reason;
    }
}
