package com.gmc.retreat.checkin.service;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.checkin.domain.CheckInEventAction;
import com.gmc.retreat.checkin.domain.CheckInMethod;
import com.gmc.retreat.checkin.domain.CheckInTokenRecord;
import com.gmc.retreat.checkin.dto.CheckInCancellationRequest;
import com.gmc.retreat.checkin.dto.CheckInRosterResponse;
import com.gmc.retreat.checkin.dto.CheckInQrCredentialResponse;
import com.gmc.retreat.checkin.dto.CheckInQrScanRequest;
import com.gmc.retreat.checkin.dto.CheckInTokenIssueRequest;
import com.gmc.retreat.checkin.dto.CheckInTokenIssueResponse;
import com.gmc.retreat.checkin.dto.CheckInTokenRevokeResponse;
import com.gmc.retreat.checkin.mapper.CheckInEventInsert;
import com.gmc.retreat.checkin.mapper.CheckInMapper;
import com.gmc.retreat.checkin.mapper.CheckInTokenInsert;
import com.gmc.retreat.error.BusinessException;
import com.gmc.retreat.error.ErrorCode;
import com.gmc.retreat.registration.dto.PageResponse;
import com.gmc.retreat.security.auth.AdminPrincipal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CheckInService {

    private static final String TOKEN_NOTICE = "This check-in token is shown only once.";

    private final CheckInMapper checkInMapper;
    private final CheckInTokenGenerator tokenGenerator;
    private final CheckInProperties properties;
    private final Clock clock;

    public CheckInService(
            CheckInMapper checkInMapper,
            CheckInTokenGenerator tokenGenerator,
            CheckInProperties properties,
            Clock clock
    ) {
        this.checkInMapper = checkInMapper;
        this.tokenGenerator = tokenGenerator;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<CheckInRosterResponse> findRoster(
            AdminPrincipal admin,
            Boolean checkedIn,
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
                checkInMapper.findRoster(
                                checkedIn,
                                retreatGroupId,
                                churchCellId,
                                normalizedKeyword,
                                safeSize,
                                safePage * safeSize
                        )
                        .stream()
                        .map(CheckInRosterResponse::from)
                        .toList(),
                safePage,
                safeSize,
                checkInMapper.countRoster(checkedIn, retreatGroupId, churchCellId, normalizedKeyword)
        );
    }

    @Transactional(readOnly = true)
    public CheckInRosterResponse findDetail(AdminPrincipal admin, Long participantId) {
        requireRole(admin, AdminRole.STAFF);
        return CheckInRosterResponse.from(findRosterItemOrThrow(participantId));
    }

    @Transactional
    public CheckInRosterResponse manuallyCheckIn(AdminPrincipal admin, Long participantId) {
        requireRole(admin, AdminRole.STAFF);
        ensureParticipantExists(participantId);
        checkInMapper.upsertCheckInIfNotCheckedIn(participantId, admin.id(), CheckInMethod.MANUAL)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECK_IN_ALREADY_COMPLETED));
        insertEvent(participantId, CheckInEventAction.CHECKED_IN, CheckInMethod.MANUAL, admin.id(), null);
        return CheckInRosterResponse.from(findRosterItemOrThrow(participantId));
    }

    @Transactional
    public CheckInRosterResponse checkInByQr(AdminPrincipal admin, CheckInQrScanRequest request) {
        requireRole(admin, AdminRole.STAFF);
        CheckInTokenRecord token = checkInMapper.findTokenByHash(hashToken(request.token().trim()))
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECK_IN_TOKEN_INVALID));
        if (token.revokedAt() != null) {
            throw new BusinessException(ErrorCode.CHECK_IN_TOKEN_REVOKED);
        }
        if (!token.expiresAt().isAfter(OffsetDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.CHECK_IN_TOKEN_EXPIRED);
        }
        checkInMapper.upsertCheckInIfNotCheckedIn(token.participantId(), admin.id(), CheckInMethod.QR)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECK_IN_ALREADY_COMPLETED));
        insertEvent(token.participantId(), CheckInEventAction.CHECKED_IN, CheckInMethod.QR, admin.id(), null);
        return CheckInRosterResponse.from(findRosterItemOrThrow(token.participantId()));
    }

    @Transactional
    public CheckInQrCredentialResponse issueParticipantQr(Long participantId) {
        ensureParticipantExists(participantId);
        OffsetDateTime expiresAt = properties.qrExpiresAt();
        if (expiresAt == null || !expiresAt.isAfter(OffsetDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.CHECK_IN_TOKEN_EXPIRED);
        }
        String token = tokenGenerator.generate();
        checkInMapper.revokeActiveTokensByParticipantId(participantId);
        checkInMapper.insertToken(new CheckInTokenInsert(participantId, hashToken(token), expiresAt, null));
        return new CheckInQrCredentialResponse(token, expiresAt);
    }

    @Transactional
    public CheckInRosterResponse cancelCheckIn(
            AdminPrincipal admin,
            Long participantId,
            CheckInCancellationRequest request
    ) {
        requireRole(admin, AdminRole.CHAIR);
        ensureParticipantExists(participantId);
        String reason = normalizeCancellationReason(request.reason());
        CheckInMethod method = checkInMapper.cancelCheckInIfCheckedIn(participantId, admin.id(), reason)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECK_IN_NOT_COMPLETED));
        insertEvent(participantId, CheckInEventAction.CANCELLED, method, admin.id(), reason);
        return CheckInRosterResponse.from(findRosterItemOrThrow(participantId));
    }

    @Transactional
    public CheckInTokenIssueResponse issueToken(
            AdminPrincipal admin,
            Long participantId,
            CheckInTokenIssueRequest request
    ) {
        requireRole(admin, AdminRole.CHAIR);
        ensureParticipantExists(participantId);
        if (!request.expiresAt().isAfter(OffsetDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        String token = tokenGenerator.generate();
        checkInMapper.revokeActiveTokensByParticipantId(participantId);
        checkInMapper.insertToken(new CheckInTokenInsert(
                participantId,
                hashToken(token),
                request.expiresAt(),
                admin.id()
        ));
        return new CheckInTokenIssueResponse(participantId, token, request.expiresAt(), TOKEN_NOTICE);
    }

    @Transactional
    public CheckInTokenRevokeResponse revokeToken(AdminPrincipal admin, Long participantId) {
        requireRole(admin, AdminRole.CHAIR);
        ensureParticipantExists(participantId);
        int revoked = checkInMapper.revokeActiveTokensByParticipantId(participantId);
        if (revoked == 0) {
            throw new BusinessException(ErrorCode.CHECK_IN_TOKEN_NOT_FOUND);
        }
        OffsetDateTime revokedAt = checkInMapper.findLatestRevokedAtByParticipantId(participantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        return new CheckInTokenRevokeResponse(participantId, revokedAt);
    }

    private void ensureParticipantExists(Long participantId) {
        if (checkInMapper.countParticipantById(participantId) == 0) {
            throw new BusinessException(ErrorCode.REGISTRATION_NOT_FOUND);
        }
    }

    private com.gmc.retreat.checkin.domain.CheckInRosterItem findRosterItemOrThrow(Long participantId) {
        return checkInMapper.findRosterItemByParticipantId(participantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGISTRATION_NOT_FOUND));
    }

    private void insertEvent(
            Long participantId,
            CheckInEventAction action,
            CheckInMethod method,
            Long adminId,
            String reason
    ) {
        checkInMapper.insertEvent(new CheckInEventInsert(participantId, action, method, adminId, reason));
    }

    private String normalizeCancellationReason(String value) {
        String reason = normalizeOptional(value);
        if (reason == null) {
            throw new BusinessException(ErrorCode.CHECK_IN_CANCELLATION_REASON_REQUIRED);
        }
        return reason;
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private void requireRole(AdminPrincipal admin, AdminRole requiredRole) {
        if (admin == null || !admin.role().hasAuthorityAtLeast(requiredRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
