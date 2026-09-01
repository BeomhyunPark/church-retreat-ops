package com.gmc.retreat.participation.service;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.common.dto.ActiveUpdateRequest;
import com.gmc.retreat.error.BusinessException;
import com.gmc.retreat.error.ErrorCode;
import com.gmc.retreat.participation.domain.ParticipationOption;
import com.gmc.retreat.participation.dto.ParticipationOptionRequest;
import com.gmc.retreat.participation.dto.ParticipationOptionResponse;
import com.gmc.retreat.participation.dto.PublicParticipationOptionResponse;
import com.gmc.retreat.participation.mapper.ParticipationOptionMapper;
import com.gmc.retreat.participation.mapper.ParticipationOptionUpsert;
import com.gmc.retreat.retreat.domain.Retreat;
import com.gmc.retreat.retreat.mapper.RetreatMapper;
import com.gmc.retreat.security.auth.AdminPrincipal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParticipationOptionService {

    private final ParticipationOptionMapper participationOptionMapper;
    private final RetreatMapper retreatMapper;

    public ParticipationOptionService(
            ParticipationOptionMapper participationOptionMapper,
            RetreatMapper retreatMapper
    ) {
        this.participationOptionMapper = participationOptionMapper;
        this.retreatMapper = retreatMapper;
    }

    @Transactional(readOnly = true)
    public List<PublicParticipationOptionResponse> findPublicOptions() {
        return participationOptionMapper.findActiveOpenOptions().stream()
                .map(PublicParticipationOptionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ParticipationOptionResponse> findAdminOptions(AdminPrincipal admin) {
        requireRole(admin, AdminRole.STAFF);
        requireCurrentRetreat();
        return participationOptionMapper.findCurrentOptions().stream()
                .map(ParticipationOptionResponse::from)
                .toList();
    }

    @Transactional
    public ParticipationOptionResponse create(AdminPrincipal admin, ParticipationOptionRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        Retreat retreat = requireCurrentRetreat();
        validateDate(retreat, request);
        ensureLabelAvailable(null, request);
        ParticipationOptionUpsert insert = toUpsert(null, request);
        participationOptionMapper.insert(insert);
        return ParticipationOptionResponse.from(findCurrentOptionOrThrow(insert.getId()));
    }

    @Transactional
    public ParticipationOptionResponse update(
            AdminPrincipal admin,
            Long id,
            ParticipationOptionRequest request
    ) {
        requireRole(admin, AdminRole.CHAIR);
        findCurrentOptionOrThrow(id);
        validateDate(requireCurrentRetreat(), request);
        ensureLabelAvailable(id, request);
        participationOptionMapper.update(toUpsert(id, request));
        return ParticipationOptionResponse.from(findCurrentOptionOrThrow(id));
    }

    @Transactional
    public ParticipationOptionResponse updateActive(
            AdminPrincipal admin,
            Long id,
            ActiveUpdateRequest request
    ) {
        requireRole(admin, AdminRole.CHAIR);
        findCurrentOptionOrThrow(id);
        participationOptionMapper.updateActive(id, request.active());
        return ParticipationOptionResponse.from(findCurrentOptionOrThrow(id));
    }

    private ParticipationOptionUpsert toUpsert(Long id, ParticipationOptionRequest request) {
        return new ParticipationOptionUpsert(
                id,
                null,
                request.optionType(),
                request.label().trim(),
                request.eventDate(),
                request.displayOrder(),
                request.active()
        );
    }

    private void validateDate(Retreat retreat, ParticipationOptionRequest request) {
        if (request.eventDate().isBefore(retreat.startsOn()) || request.eventDate().isAfter(retreat.endsOn())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void ensureLabelAvailable(Long id, ParticipationOptionRequest request) {
        participationOptionMapper.findCurrentOptionIdByDateAndLabel(request.eventDate(), request.label().trim())
                .filter(existingId -> id == null || !existingId.equals(id))
                .ifPresent(existingId -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_PARTICIPATION_OPTION);
                });
    }

    private Retreat requireCurrentRetreat() {
        return retreatMapper.findCurrent()
                .orElseThrow(() -> new BusinessException(ErrorCode.RETREAT_NOT_FOUND));
    }

    private ParticipationOption findCurrentOptionOrThrow(Long id) {
        return participationOptionMapper.findCurrentOptionById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPATION_OPTION_NOT_FOUND));
    }

    private void requireRole(AdminPrincipal admin, AdminRole requiredRole) {
        if (admin == null || !admin.role().hasAuthorityAtLeast(requiredRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
