package com.gmc.retreat.fee.dto;

import com.gmc.retreat.fee.domain.FeeEvent;
import com.gmc.retreat.fee.domain.FeeRosterItem;
import java.util.List;

public record FeeDetailResponse(
        FeeRosterResponse participant,
        List<FeeEventResponse> events
) {
    public static FeeDetailResponse from(FeeRosterItem item, List<FeeEvent> events) {
        return new FeeDetailResponse(
                FeeRosterResponse.from(item),
                events.stream()
                        .map(FeeEventResponse::from)
                        .toList()
        );
    }
}
