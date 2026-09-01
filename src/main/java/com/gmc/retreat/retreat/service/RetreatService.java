package com.gmc.retreat.retreat.service;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.error.BusinessException;
import com.gmc.retreat.error.ErrorCode;
import com.gmc.retreat.participation.mapper.ParticipationOptionMapper;
import com.gmc.retreat.schedule.mapper.ScheduleItemMapper;
import com.gmc.retreat.retreat.domain.Retreat;
import com.gmc.retreat.retreat.domain.RetreatStatus;
import com.gmc.retreat.retreat.dto.RetreatCreateRequest;
import com.gmc.retreat.retreat.dto.RetreatResponse;
import com.gmc.retreat.retreat.dto.RetreatRegistrationOpenUpdateRequest;
import com.gmc.retreat.retreat.dto.RetreatStatusUpdateRequest;
import com.gmc.retreat.retreat.dto.RetreatUpdateRequest;
import com.gmc.retreat.retreat.mapper.RetreatInsert;
import com.gmc.retreat.retreat.mapper.RetreatMapper;
import com.gmc.retreat.security.auth.AdminPrincipal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RetreatService {

    private final RetreatMapper retreatMapper;
    private final ScheduleItemMapper scheduleItemMapper;
    private final ParticipationOptionMapper participationOptionMapper;

    public RetreatService(
            RetreatMapper retreatMapper,
            ScheduleItemMapper scheduleItemMapper,
            ParticipationOptionMapper participationOptionMapper
    ) {
        this.retreatMapper = retreatMapper;
        this.scheduleItemMapper = scheduleItemMapper;
        this.participationOptionMapper = participationOptionMapper;
    }

    @Transactional(readOnly = true)
    public List<RetreatResponse> findAll(AdminPrincipal admin) {
        requireRole(admin, AdminRole.STAFF);
        return retreatMapper.findAll().stream().map(RetreatResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RetreatResponse findCurrent(AdminPrincipal admin) {
        requireRole(admin, AdminRole.STAFF);
        return RetreatResponse.from(retreatMapper.findCurrent()
                .orElseThrow(() -> new BusinessException(ErrorCode.RETREAT_NOT_FOUND)));
    }

    @Transactional
    public RetreatResponse create(AdminPrincipal admin, RetreatCreateRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        validateDateRange(request.startsOn(), request.endsOn());
        if (retreatMapper.findCurrent().isPresent()) {
            throw new BusinessException(ErrorCode.CURRENT_RETREAT_ALREADY_EXISTS);
        }

        RetreatInsert insert = new RetreatInsert(
                normalizeName(request.name()),
                request.startsOn(),
                request.endsOn()
        );
        retreatMapper.insert(insert);
        return RetreatResponse.from(findByIdOrThrow(insert.getId()));
    }

    @Transactional
    public RetreatResponse update(AdminPrincipal admin, Long id, RetreatUpdateRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        Retreat retreat = findByIdOrThrow(id);
        if (retreat.status() == RetreatStatus.CLOSED) {
            throw new BusinessException(ErrorCode.INVALID_RETREAT_STATUS_TRANSITION);
        }
        validateDateRange(request.startsOn(), request.endsOn());
        if (!retreat.startsOn().equals(request.startsOn()) || !retreat.endsOn().equals(request.endsOn())) {
            long dayShift = ChronoUnit.DAYS.between(retreat.startsOn(), request.startsOn());
            scheduleItemMapper.shiftScheduleItems(
                    id, dayShift, request.startsOn(), request.endsOn(), admin.id()
            );
            participationOptionMapper.shiftUnlinkedOptions(
                    id, dayShift, request.startsOn(), request.endsOn()
            );
            scheduleItemMapper.syncLinkedParticipationOptions(id);
        }
        retreatMapper.updateMetadata(
                id,
                normalizeName(request.name()),
                request.startsOn(),
                request.endsOn()
        );
        return RetreatResponse.from(findByIdOrThrow(id));
    }

    @Transactional
    public RetreatResponse updateStatus(
            AdminPrincipal admin,
            Long id,
            RetreatStatusUpdateRequest request
    ) {
        requireRole(admin, AdminRole.CHAIR);
        Retreat retreat = findByIdOrThrow(id);
        RetreatStatus next = request.status();

        if (retreat.status() == RetreatStatus.DRAFT && next == RetreatStatus.OPEN) {
            retreatMapper.updateStatus(id, RetreatStatus.OPEN, null);
        } else if (retreat.status() == RetreatStatus.OPEN && next == RetreatStatus.CLOSED) {
            int participantCount = retreatMapper.countRegisteredParticipants(id);
            retreatMapper.updateStatus(id, RetreatStatus.CLOSED, participantCount);
        } else {
            throw new BusinessException(ErrorCode.INVALID_RETREAT_STATUS_TRANSITION);
        }

        return RetreatResponse.from(findByIdOrThrow(id));
    }

    @Transactional
    public RetreatResponse updateRegistrationOpen(
            AdminPrincipal admin,
            Long id,
            RetreatRegistrationOpenUpdateRequest request
    ) {
        requireRole(admin, AdminRole.CHAIR);
        Retreat retreat = findByIdOrThrow(id);
        if (retreat.status() != RetreatStatus.OPEN) {
            throw new BusinessException(ErrorCode.INVALID_RETREAT_STATUS_TRANSITION);
        }
        retreatMapper.updateRegistrationOpen(id, request.registrationOpen());
        return RetreatResponse.from(findByIdOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Long requireOpenRetreatId() {
        return retreatMapper.findRegistrationOpen()
                .map(Retreat::id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGISTRATION_NOT_OPEN));
    }

    @Transactional(readOnly = true)
    public Long requireOperationalRetreatId() {
        return retreatMapper.findOpen()
                .map(Retreat::id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGISTRATION_EDIT_CLOSED));
    }

    @Transactional(readOnly = true)
    public Long requireCurrentRetreatId() {
        return retreatMapper.findCurrent()
                .map(Retreat::id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETREAT_NOT_FOUND));
    }

    private Retreat findByIdOrThrow(Long id) {
        return retreatMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETREAT_NOT_FOUND));
    }

    private void validateDateRange(LocalDate startsOn, LocalDate endsOn) {
        if (endsOn.isBefore(startsOn)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Retreat end date must not precede start date.");
        }
    }

    private String normalizeName(String name) {
        String normalized = name.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    private void requireRole(AdminPrincipal admin, AdminRole requiredRole) {
        if (admin == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!admin.role().hasAuthorityAtLeast(requiredRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
