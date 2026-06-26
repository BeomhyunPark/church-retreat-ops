package com.gmc.retreat.announcement.service;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.announcement.domain.Announcement;
import com.gmc.retreat.announcement.domain.AnnouncementTarget;
import com.gmc.retreat.announcement.domain.AnnouncementTargetType;
import com.gmc.retreat.announcement.dto.AnnouncementRequest;
import com.gmc.retreat.announcement.dto.AnnouncementResponse;
import com.gmc.retreat.announcement.dto.AnnouncementTargetRequest;
import com.gmc.retreat.announcement.dto.PinnedUpdateRequest;
import com.gmc.retreat.announcement.mapper.AnnouncementMapper;
import com.gmc.retreat.announcement.mapper.AnnouncementTargetInsert;
import com.gmc.retreat.announcement.mapper.AnnouncementUpsert;
import com.gmc.retreat.community.dto.ActiveUpdateRequest;
import com.gmc.retreat.community.mapper.CommunityMapper;
import com.gmc.retreat.error.BusinessException;
import com.gmc.retreat.error.ErrorCode;
import com.gmc.retreat.registration.domain.RegistrationStatus;
import com.gmc.retreat.retreat.mapper.RetreatGroupMapper;
import com.gmc.retreat.security.auth.AdminPrincipal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AnnouncementService {

    private static final Set<String> PAYMENT_STATUS_VALUES = Set.of("PAID", "UNPAID");
    private static final Set<String> BOOLEAN_TARGET_VALUES = Set.of("TRUE", "FALSE");

    private final AnnouncementMapper announcementMapper;
    private final CommunityMapper communityMapper;
    private final RetreatGroupMapper retreatGroupMapper;

    public AnnouncementService(
            AnnouncementMapper announcementMapper,
            CommunityMapper communityMapper,
            RetreatGroupMapper retreatGroupMapper
    ) {
        this.announcementMapper = announcementMapper;
        this.communityMapper = communityMapper;
        this.retreatGroupMapper = retreatGroupMapper;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> findAnnouncements(AdminPrincipal admin) {
        requireRole(admin, AdminRole.STAFF);
        return announcementMapper.findAnnouncements()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnnouncementResponse findAnnouncement(AdminPrincipal admin, Long id) {
        requireRole(admin, AdminRole.STAFF);
        return toResponse(findAnnouncementOrThrow(id));
    }

    @Transactional
    public AnnouncementResponse createAnnouncement(AdminPrincipal admin, AnnouncementRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        ValidatedAnnouncement validated = validateRequest(request);
        AnnouncementUpsert insert = new AnnouncementUpsert(
                null,
                validated.title(),
                validated.content(),
                request.pinned(),
                request.active(),
                request.visibleFrom(),
                request.visibleUntil(),
                admin.id()
        );
        announcementMapper.insertAnnouncement(insert);
        replaceTargets(insert.getId(), validated.targets());
        return toResponse(findAnnouncementOrThrow(insert.getId()));
    }

    @Transactional
    public AnnouncementResponse updateAnnouncement(AdminPrincipal admin, Long id, AnnouncementRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        findAnnouncementOrThrow(id);
        ValidatedAnnouncement validated = validateRequest(request);
        announcementMapper.updateAnnouncement(new AnnouncementUpsert(
                id,
                validated.title(),
                validated.content(),
                request.pinned(),
                request.active(),
                request.visibleFrom(),
                request.visibleUntil(),
                admin.id()
        ));
        replaceTargets(id, validated.targets());
        return toResponse(findAnnouncementOrThrow(id));
    }

    @Transactional
    public AnnouncementResponse updateActive(AdminPrincipal admin, Long id, ActiveUpdateRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        findAnnouncementOrThrow(id);
        announcementMapper.updateActive(id, request.active(), admin.id());
        return toResponse(findAnnouncementOrThrow(id));
    }

    @Transactional
    public AnnouncementResponse updatePinned(AdminPrincipal admin, Long id, PinnedUpdateRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        findAnnouncementOrThrow(id);
        announcementMapper.updatePinned(id, request.pinned(), admin.id());
        return toResponse(findAnnouncementOrThrow(id));
    }

    private AnnouncementResponse toResponse(Announcement announcement) {
        return AnnouncementResponse.from(
                announcement,
                announcementMapper.findTargetsByAnnouncementId(announcement.id())
        );
    }

    private Announcement findAnnouncementOrThrow(Long id) {
        return announcementMapper.findAnnouncementById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
    }

    private void replaceTargets(Long announcementId, List<ValidatedTarget> targets) {
        announcementMapper.deleteTargetsByAnnouncementId(announcementId);
        targets.forEach(target -> announcementMapper.insertTarget(new AnnouncementTargetInsert(
                announcementId,
                target.targetType(),
                target.targetValue()
        )));
    }

    private ValidatedAnnouncement validateRequest(AnnouncementRequest request) {
        String title = normalizeRequired(request.title());
        String content = normalizeRequired(request.content());
        validateVisiblePeriod(request.visibleFrom(), request.visibleUntil());

        List<ValidatedTarget> targets = request.targets()
                .stream()
                .map(this::validateTarget)
                .toList();
        ensureNoDuplicateTargets(targets);

        return new ValidatedAnnouncement(title, content, targets);
    }

    private void validateVisiblePeriod(OffsetDateTime visibleFrom, OffsetDateTime visibleUntil) {
        if (visibleFrom != null && visibleUntil != null && visibleUntil.isBefore(visibleFrom)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private ValidatedTarget validateTarget(AnnouncementTargetRequest target) {
        AnnouncementTargetType type = target.targetType();
        if (type == AnnouncementTargetType.ALL) {
            if (StringUtils.hasText(target.targetValue())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            return new ValidatedTarget(type, null);
        }

        String value = normalizeRequired(target.targetValue());
        return switch (type) {
            case REGISTRATION_STATUS -> new ValidatedTarget(type, validateRegistrationStatus(value));
            case PAYMENT_STATUS -> new ValidatedTarget(type, validateValueIn(value, PAYMENT_STATUS_VALUES));
            case NEWCOMER, CARE_TARGET -> new ValidatedTarget(type, validateValueIn(value, BOOLEAN_TARGET_VALUES));
            case CHURCH_MIDDLE_GROUP -> new ValidatedTarget(type, validateMiddleGroupId(value));
            case CHURCH_CELL -> new ValidatedTarget(type, validateCellId(value));
            case RETREAT_GROUP -> new ValidatedTarget(type, validateRetreatGroupId(value));
            case ADMIN_ROLE -> new ValidatedTarget(type, validateAdminRole(value));
            case ALL -> throw new BusinessException(ErrorCode.INVALID_REQUEST);
        };
    }

    private String validateRegistrationStatus(String value) {
        String normalized = value.toUpperCase(Locale.ROOT);
        try {
            RegistrationStatus.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String validateAdminRole(String value) {
        String normalized = value.toUpperCase(Locale.ROOT);
        try {
            AdminRole.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String validateValueIn(String value, Set<String> allowedValues) {
        String normalized = value.toUpperCase(Locale.ROOT);
        if (!allowedValues.contains(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    private String validateMiddleGroupId(String value) {
        Long id = parsePositiveId(value);
        if (communityMapper.findMiddleGroupById(id).isEmpty()) {
            throw new BusinessException(ErrorCode.COMMUNITY_NOT_FOUND);
        }
        return id.toString();
    }

    private String validateCellId(String value) {
        Long id = parsePositiveId(value);
        if (communityMapper.findCellById(id).isEmpty()) {
            throw new BusinessException(ErrorCode.COMMUNITY_NOT_FOUND);
        }
        return id.toString();
    }

    private String validateRetreatGroupId(String value) {
        Long id = parsePositiveId(value);
        if (retreatGroupMapper.findGroupById(id).isEmpty()) {
            throw new BusinessException(ErrorCode.RETREAT_GROUP_NOT_FOUND);
        }
        return id.toString();
    }

    private Long parsePositiveId(String value) {
        try {
            Long id = Long.valueOf(value);
            if (id <= 0) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void ensureNoDuplicateTargets(List<ValidatedTarget> targets) {
        Set<String> seen = new HashSet<>();
        for (ValidatedTarget target : targets) {
            String key = target.targetType().name() + ":" + (target.targetValue() == null ? "" : target.targetValue());
            if (!seen.add(key)) {
                throw new BusinessException(ErrorCode.DUPLICATE_ANNOUNCEMENT_TARGET);
            }
        }
    }

    private String normalizeRequired(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return value.trim();
    }

    private void requireRole(AdminPrincipal admin, AdminRole requiredRole) {
        if (admin == null || !admin.role().hasAuthorityAtLeast(requiredRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private record ValidatedAnnouncement(
            String title,
            String content,
            List<ValidatedTarget> targets
    ) {
    }

    private record ValidatedTarget(
            AnnouncementTargetType targetType,
            String targetValue
    ) {
    }
}
