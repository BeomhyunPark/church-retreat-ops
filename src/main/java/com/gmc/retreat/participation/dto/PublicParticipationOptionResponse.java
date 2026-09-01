package com.gmc.retreat.participation.dto;

import com.gmc.retreat.participation.domain.ParticipationOption;
import com.gmc.retreat.participation.domain.ParticipationOptionType;
import java.time.LocalDate;

public record PublicParticipationOptionResponse(
        Long id,
        ParticipationOptionType optionType,
        String label,
        LocalDate eventDate,
        Integer displayOrder
) {
    public static PublicParticipationOptionResponse from(ParticipationOption option) {
        return new PublicParticipationOptionResponse(
                option.id(), option.optionType(), option.label(), option.eventDate(), option.displayOrder()
        );
    }
}
