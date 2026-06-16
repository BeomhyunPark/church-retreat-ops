package com.gmc.retreat.fee.service;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.error.BusinessException;
import com.gmc.retreat.error.ErrorCode;
import com.gmc.retreat.fee.domain.FeeEvent;
import com.gmc.retreat.fee.domain.FeeRosterItem;
import com.gmc.retreat.fee.dto.FeeDetailResponse;
import com.gmc.retreat.fee.dto.FeeEventResponse;
import com.gmc.retreat.fee.dto.FeeRosterResponse;
import com.gmc.retreat.fee.dto.FeeStatusUpdateRequest;
import com.gmc.retreat.fee.mapper.FeeMapper;
import com.gmc.retreat.fee.mapper.FeeMapper.FeeEventInsert;
import com.gmc.retreat.registration.dto.PageResponse;
import com.gmc.retreat.security.auth.AdminPrincipal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FeeManagementService {

    private static final int DETAIL_EVENT_LIMIT = 10;
    private static final int EVENT_LIST_LIMIT = 100;

    private final FeeMapper feeMapper;

    public FeeManagementService(FeeMapper feeMapper) {
        this.feeMapper = feeMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<FeeRosterResponse> findRoster(
            AdminPrincipal admin,
            Boolean feePaid,
            Long retreatGroupId,
            Long churchCellId,
            String keyword,
            int page,
            int size
    ) {
        requireRole(admin, AdminRole.STAFF);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String normalizedKeyword = normalizeOptional(keyword);
        return PageResponse.of(
                feeMapper.findRoster(
                                feePaid,
                                retreatGroupId,
                                churchCellId,
                                normalizedKeyword,
                                safeSize,
                                safePage * safeSize
                        )
                        .stream()
                        .map(FeeRosterResponse::from)
                        .toList(),
                safePage,
                safeSize,
                feeMapper.countRoster(feePaid, retreatGroupId, churchCellId, normalizedKeyword)
        );
    }

    @Transactional(readOnly = true)
    public FeeDetailResponse findDetail(AdminPrincipal admin, Long participantId) {
        requireRole(admin, AdminRole.STAFF);
        FeeRosterItem item = findRosterItemOrThrow(participantId);
        return FeeDetailResponse.from(item, feeMapper.findEventsByParticipantId(participantId, DETAIL_EVENT_LIMIT));
    }

    @Transactional(readOnly = true)
    public List<FeeEventResponse> findEvents(AdminPrincipal admin, Long participantId) {
        requireRole(admin, AdminRole.STAFF);
        ensureParticipantExists(participantId);
        return feeMapper.findEventsByParticipantId(participantId, EVENT_LIST_LIMIT)
                .stream()
                .map(FeeEventResponse::from)
                .toList();
    }

    @Transactional
    public FeeDetailResponse updateFeeStatus(
            AdminPrincipal admin,
            Long participantId,
            FeeStatusUpdateRequest request
    ) {
        requireRole(admin, AdminRole.CHAIR);
        String reason = normalizeReason(request.feePaid(), request.reason());
        Boolean previousFeePaid = feeMapper.updateFeePaidIfChanged(participantId, request.feePaid(), admin.id())
                .orElseThrow(() -> noFeeChangeException(participantId, request.feePaid()));
        feeMapper.insertEvent(new FeeEventInsert(
                participantId,
                previousFeePaid,
                request.feePaid(),
                admin.id(),
                reason
        ));
        FeeRosterItem item = findRosterItemOrThrow(participantId);
        List<FeeEvent> events = feeMapper.findEventsByParticipantId(participantId, DETAIL_EVENT_LIMIT);
        return FeeDetailResponse.from(item, events);
    }

    private BusinessException noFeeChangeException(Long participantId, Boolean requestedFeePaid) {
        Boolean currentFeePaid = feeMapper.findFeePaidByParticipantId(participantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGISTRATION_NOT_FOUND));
        if (Boolean.TRUE.equals(currentFeePaid) && Boolean.TRUE.equals(requestedFeePaid)) {
            return new BusinessException(ErrorCode.FEE_ALREADY_PAID);
        }
        if (Boolean.FALSE.equals(currentFeePaid) && Boolean.FALSE.equals(requestedFeePaid)) {
            return new BusinessException(ErrorCode.FEE_ALREADY_UNPAID);
        }
        return new BusinessException(ErrorCode.INVALID_REQUEST);
    }

    private void ensureParticipantExists(Long participantId) {
        if (feeMapper.countParticipantById(participantId) == 0) {
            throw new BusinessException(ErrorCode.REGISTRATION_NOT_FOUND);
        }
    }

    private FeeRosterItem findRosterItemOrThrow(Long participantId) {
        return feeMapper.findRosterItemByParticipantId(participantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGISTRATION_NOT_FOUND));
    }

    private String normalizeReason(Boolean feePaid, String value) {
        String reason = normalizeOptional(value);
        if (Boolean.FALSE.equals(feePaid) && reason == null) {
            throw new BusinessException(ErrorCode.FEE_REVERT_REASON_REQUIRED);
        }
        return reason;
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
}
