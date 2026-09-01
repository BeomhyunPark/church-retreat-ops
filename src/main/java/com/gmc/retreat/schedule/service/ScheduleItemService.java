package com.gmc.retreat.schedule.service;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.community.dto.ActiveUpdateRequest;
import com.gmc.retreat.error.BusinessException;
import com.gmc.retreat.error.ErrorCode;
import com.gmc.retreat.schedule.domain.ScheduleCategory;
import com.gmc.retreat.schedule.domain.ScheduleItem;
import com.gmc.retreat.schedule.domain.ScheduleTargetAudience;
import com.gmc.retreat.schedule.dto.ScheduleItemRequest;
import com.gmc.retreat.schedule.dto.ScheduleItemResponse;
import com.gmc.retreat.schedule.mapper.ScheduleItemMapper;
import com.gmc.retreat.schedule.mapper.ScheduleItemUpsert;
import com.gmc.retreat.security.auth.AdminPrincipal;
import com.gmc.retreat.retreat.service.RetreatService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ScheduleItemService {

    private final ScheduleItemMapper scheduleItemMapper;
    private final RetreatService retreatService;

    public ScheduleItemService(ScheduleItemMapper scheduleItemMapper, RetreatService retreatService) {
        this.scheduleItemMapper = scheduleItemMapper;
        this.retreatService = retreatService;
    }

    @Transactional(readOnly = true)
    public List<ScheduleItemResponse> findScheduleItems(
            AdminPrincipal admin,
            Long retreatId,
            LocalDate scheduleDate,
            ScheduleCategory category,
            Boolean active
    ) {
        requireRole(admin, AdminRole.STAFF);
        return scheduleItemMapper.findScheduleItems(retreatId, scheduleDate, category, active)
                .stream()
                .map(ScheduleItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScheduleItemResponse findScheduleItem(AdminPrincipal admin, Long id) {
        requireRole(admin, AdminRole.STAFF);
        return ScheduleItemResponse.from(findScheduleItemOrThrow(id));
    }

    @Transactional
    public ScheduleItemResponse createScheduleItem(AdminPrincipal admin, ScheduleItemRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        retreatService.requireCurrentRetreatId();
        ValidatedScheduleItem validated = validateRequest(request);
        ScheduleItemUpsert insert = new ScheduleItemUpsert(
                null,
                validated.title(),
                validated.description(),
                request.scheduleDate(),
                request.startsAt(),
                request.endsAt(),
                validated.location(),
                request.category(),
                request.targetAudience(),
                request.active(),
                request.displayOrder(),
                admin.id()
        );
        scheduleItemMapper.insertScheduleItem(insert);
        return ScheduleItemResponse.from(findScheduleItemOrThrow(insert.getId()));
    }

    @Transactional
    public ScheduleItemResponse updateScheduleItem(AdminPrincipal admin, Long id, ScheduleItemRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        findScheduleItemOrThrow(id);
        ValidatedScheduleItem validated = validateRequest(request);
        scheduleItemMapper.updateScheduleItem(new ScheduleItemUpsert(
                id,
                validated.title(),
                validated.description(),
                request.scheduleDate(),
                request.startsAt(),
                request.endsAt(),
                validated.location(),
                request.category(),
                request.targetAudience(),
                request.active(),
                request.displayOrder(),
                admin.id()
        ));
        return ScheduleItemResponse.from(findScheduleItemOrThrow(id));
    }

    @Transactional
    public ScheduleItemResponse updateActive(AdminPrincipal admin, Long id, ActiveUpdateRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        findScheduleItemOrThrow(id);
        scheduleItemMapper.updateActive(id, request.active(), admin.id());
        return ScheduleItemResponse.from(findScheduleItemOrThrow(id));
    }

    private ScheduleItem findScheduleItemOrThrow(Long id) {
        return scheduleItemMapper.findScheduleItemById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_ITEM_NOT_FOUND));
    }

    private ValidatedScheduleItem validateRequest(ScheduleItemRequest request) {
        validateScheduleDate(request.scheduleDate(), request.startsAt(), request.endsAt());
        validateTimeRange(request.startsAt(), request.endsAt());
        return new ValidatedScheduleItem(
                normalizeRequired(request.title()),
                normalizeOptional(request.description()),
                normalizeOptional(request.location())
        );
    }

    private void validateScheduleDate(LocalDate scheduleDate, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (!scheduleDate.equals(startsAt.toLocalDate()) || !scheduleDate.equals(endsAt.toLocalDate())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateTimeRange(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String normalizeRequired(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void requireRole(AdminPrincipal admin, AdminRole requiredRole) {
        if (admin == null || !admin.role().hasAuthorityAtLeast(requiredRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private record ValidatedScheduleItem(
            String title,
            String description,
            String location
    ) {
    }
}
