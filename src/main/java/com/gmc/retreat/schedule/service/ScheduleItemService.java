package com.gmc.retreat.schedule.service;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.common.dto.ActiveUpdateRequest;
import com.gmc.retreat.error.BusinessException;
import com.gmc.retreat.error.ErrorCode;
import com.gmc.retreat.participation.domain.ParticipationOption;
import com.gmc.retreat.participation.domain.ParticipationOptionType;
import com.gmc.retreat.participation.mapper.ParticipationOptionMapper;
import com.gmc.retreat.participation.mapper.ParticipationOptionUpsert;
import com.gmc.retreat.retreat.domain.Retreat;
import com.gmc.retreat.retreat.mapper.RetreatMapper;
import com.gmc.retreat.schedule.domain.ScheduleCategory;
import com.gmc.retreat.schedule.domain.ScheduleItem;
import com.gmc.retreat.schedule.domain.ScheduleTargetAudience;
import com.gmc.retreat.schedule.dto.ScheduleItemRequest;
import com.gmc.retreat.schedule.dto.ScheduleItemResponse;
import com.gmc.retreat.schedule.mapper.ScheduleItemMapper;
import com.gmc.retreat.schedule.mapper.ScheduleItemUpsert;
import com.gmc.retreat.security.auth.AdminPrincipal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ScheduleItemService {

    private static final ZoneId RETREAT_ZONE = ZoneId.of("Asia/Seoul");

    private final ScheduleItemMapper scheduleItemMapper;
    private final ParticipationOptionMapper participationOptionMapper;
    private final RetreatMapper retreatMapper;

    public ScheduleItemService(
            ScheduleItemMapper scheduleItemMapper,
            ParticipationOptionMapper participationOptionMapper,
            RetreatMapper retreatMapper
    ) {
        this.scheduleItemMapper = scheduleItemMapper;
        this.participationOptionMapper = participationOptionMapper;
        this.retreatMapper = retreatMapper;
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
        Retreat retreat = requireCurrentRetreat();
        ValidatedScheduleItem validated = validateRequest(request, retreat);
        ensureOptionLabelAvailable(null, request);
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
                request.collectParticipation(),
                admin.id()
        );
        scheduleItemMapper.insertScheduleItem(insert);
        syncParticipationOption(insert.getId(), request);
        return ScheduleItemResponse.from(findScheduleItemOrThrow(insert.getId()));
    }

    @Transactional
    public ScheduleItemResponse updateScheduleItem(AdminPrincipal admin, Long id, ScheduleItemRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        findScheduleItemOrThrow(id);
        ValidatedScheduleItem validated = validateRequest(request, requireCurrentRetreat());
        Long existingOptionId = participationOptionMapper.findCurrentOptionByScheduleItemId(id)
                .map(ParticipationOption::id)
                .orElse(null);
        ensureOptionLabelAvailable(existingOptionId, request);
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
                request.collectParticipation(),
                admin.id()
        ));
        syncParticipationOption(id, request);
        return ScheduleItemResponse.from(findScheduleItemOrThrow(id));
    }

    @Transactional
    public ScheduleItemResponse updateActive(AdminPrincipal admin, Long id, ActiveUpdateRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        ScheduleItem scheduleItem = findScheduleItemOrThrow(id);
        scheduleItemMapper.updateActive(id, request.active(), admin.id());
        participationOptionMapper.findCurrentOptionByScheduleItemId(id)
                .ifPresent(option -> participationOptionMapper.updateActive(
                        option.id(), request.active() && scheduleItem.collectParticipation()
                ));
        return ScheduleItemResponse.from(findScheduleItemOrThrow(id));
    }

    private ScheduleItem findScheduleItemOrThrow(Long id) {
        return scheduleItemMapper.findScheduleItemById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_ITEM_NOT_FOUND));
    }

    private ValidatedScheduleItem validateRequest(ScheduleItemRequest request, Retreat retreat) {
        if (request.scheduleDate().isBefore(retreat.startsOn()) || request.scheduleDate().isAfter(retreat.endsOn())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        validateScheduleTime(request.scheduleDate(), request.startsAt(), request.endsAt());
        validateTimeRange(request.startsAt(), request.endsAt());
        if (request.collectParticipation() && request.targetAudience() != ScheduleTargetAudience.ALL) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return new ValidatedScheduleItem(
                normalizeRequired(request.title()),
                normalizeOptional(request.description()),
                normalizeOptional(request.location())
        );
    }

    private void validateScheduleTime(LocalDate scheduleDate, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (startsAt == null && endsAt == null) {
            return;
        }
        if (startsAt == null || endsAt == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (!scheduleDate.equals(startsAt.atZoneSameInstant(RETREAT_ZONE).toLocalDate())
                || !scheduleDate.equals(endsAt.atZoneSameInstant(RETREAT_ZONE).toLocalDate())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateTimeRange(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (startsAt == null) {
            return;
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void ensureOptionLabelAvailable(Long existingOptionId, ScheduleItemRequest request) {
        if (!request.collectParticipation() && existingOptionId == null) {
            return;
        }
        participationOptionMapper.findCurrentOptionIdByDateAndLabel(
                        request.scheduleDate(), request.title().trim()
                )
                .filter(foundId -> existingOptionId == null || !foundId.equals(existingOptionId))
                .ifPresent(foundId -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_PARTICIPATION_OPTION);
                });
    }

    private void syncParticipationOption(Long scheduleItemId, ScheduleItemRequest request) {
        ParticipationOption existing = participationOptionMapper
                .findCurrentOptionByScheduleItemId(scheduleItemId)
                .orElse(null);
        if (existing == null && !request.collectParticipation()) {
            return;
        }

        ParticipationOptionUpsert option = new ParticipationOptionUpsert(
                existing == null ? null : existing.id(),
                scheduleItemId,
                request.category() == ScheduleCategory.MEAL
                        ? ParticipationOptionType.MEAL
                        : ParticipationOptionType.PROGRAM,
                request.title().trim(),
                request.scheduleDate(),
                request.displayOrder(),
                request.active() && request.collectParticipation()
        );
        if (existing == null) {
            participationOptionMapper.insert(option);
        } else {
            participationOptionMapper.update(option);
        }
    }

    private Retreat requireCurrentRetreat() {
        return retreatMapper.findCurrent()
                .orElseThrow(() -> new BusinessException(ErrorCode.RETREAT_NOT_FOUND));
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
