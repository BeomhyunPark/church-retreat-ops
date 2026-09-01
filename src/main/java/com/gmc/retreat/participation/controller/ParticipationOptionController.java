package com.gmc.retreat.participation.controller;

import com.gmc.retreat.api.ApiResponse;
import com.gmc.retreat.participation.dto.PublicParticipationOptionResponse;
import com.gmc.retreat.participation.service.ParticipationOptionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/participation-options")
public class ParticipationOptionController {

    private final ParticipationOptionService participationOptionService;

    public ParticipationOptionController(ParticipationOptionService participationOptionService) {
        this.participationOptionService = participationOptionService;
    }

    @GetMapping
    public ApiResponse<List<PublicParticipationOptionResponse>> options() {
        return ApiResponse.success(participationOptionService.findPublicOptions());
    }
}
