package com.gmc.retreat.registration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.community.service.CommunityService;
import com.gmc.retreat.error.BusinessException;
import com.gmc.retreat.error.ErrorCode;
import com.gmc.retreat.registration.domain.Registration;
import com.gmc.retreat.registration.domain.RegistrationActorType;
import com.gmc.retreat.registration.domain.RegistrationHistoryChangeType;
import com.gmc.retreat.registration.domain.RegistrationStatus;
import com.gmc.retreat.registration.dto.AdminParticipantChurchCellUpdateRequest;
import com.gmc.retreat.registration.dto.AdminRegistrationFeePaidUpdateRequest;
import com.gmc.retreat.registration.dto.AdminRegistrationManagementUpdateRequest;
import com.gmc.retreat.registration.dto.AdminRegistrationResponse;
import com.gmc.retreat.registration.dto.AdminRegistrationStatusUpdateRequest;
import com.gmc.retreat.registration.dto.PageResponse;
import com.gmc.retreat.registration.dto.RegistrationCreateRequest;
import com.gmc.retreat.registration.dto.RegistrationCreateResponse;
import com.gmc.retreat.registration.dto.RegistrationCreateResponse.ResultType;
import com.gmc.retreat.registration.dto.RegistrationHistoryResponse;
import com.gmc.retreat.registration.dto.RegistrationResponse;
import com.gmc.retreat.registration.dto.RegistrationSelfLookupRequest;
import com.gmc.retreat.registration.dto.RegistrationSelfUpdateRequest;
import com.gmc.retreat.registration.mapper.RegistrationHistoryMapper;
import com.gmc.retreat.registration.mapper.RegistrationHistoryMapper.RegistrationHistoryInsert;
import com.gmc.retreat.registration.mapper.RegistrationMapper;
import com.gmc.retreat.registration.mapper.RegistrationMapper.RegistrationInsert;
import com.gmc.retreat.registration.mapper.RegistrationMapper.RegistrationManagementUpdate;
import com.gmc.retreat.registration.mapper.RegistrationMapper.RegistrationOverwrite;
import com.gmc.retreat.registration.mapper.RegistrationMapper.RegistrationSelfUpdate;
import com.gmc.retreat.registration.mapper.RegistrationPrivacyAccessLogMapper;
import com.gmc.retreat.registration.mapper.RegistrationPrivacyAccessLogMapper.RegistrationPrivacyAccessLogInsert;
import com.gmc.retreat.security.auth.AdminPrincipal;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RegistrationService {

    private static final String LOOKUP_KEY_NOTICE =
            "This lookup key is shown only once. Please save it safely.";
    private static final String DETAIL_VIEW = "DETAIL_VIEW";
    private static final String HISTORY_VIEW = "HISTORY_VIEW";

    private final RegistrationMapper registrationMapper;
    private final RegistrationHistoryMapper registrationHistoryMapper;
    private final RegistrationPrivacyAccessLogMapper privacyAccessLogMapper;
    private final CommunityService communityService;
    private final LookupKeyGenerator lookupKeyGenerator;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationProperties registrationProperties;
    private final ObjectMapper objectMapper;

    public RegistrationService(
            RegistrationMapper registrationMapper,
            RegistrationHistoryMapper registrationHistoryMapper,
            RegistrationPrivacyAccessLogMapper privacyAccessLogMapper,
            CommunityService communityService,
            LookupKeyGenerator lookupKeyGenerator,
            PasswordEncoder passwordEncoder,
            RegistrationProperties registrationProperties,
            ObjectMapper objectMapper
    ) {
        this.registrationMapper = registrationMapper;
        this.registrationHistoryMapper = registrationHistoryMapper;
        this.privacyAccessLogMapper = privacyAccessLogMapper;
        this.communityService = communityService;
        this.lookupKeyGenerator = lookupKeyGenerator;
        this.passwordEncoder = passwordEncoder;
        this.registrationProperties = registrationProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RegistrationCreateResponse createOrOverwrite(RegistrationCreateRequest request) {
        String name = normalizeName(request.name());
        String normalizedName = normalizeName(request.name());
        String phoneNumber = PhoneNumberNormalizer.normalize(request.phoneNumber());
        String phoneLastFour = PhoneNumberNormalizer.lastFour(phoneNumber);
        String lookupKey = lookupKeyGenerator.generate();
        String lookupKeyHash = passwordEncoder.encode(lookupKey);
        String churchCellDepartment = normalizeOptional(request.churchCellDepartment());

        Registration existing = registrationMapper.findActiveByNormalizedNameAndPhoneNumber(normalizedName, phoneNumber)
                .orElse(null);

        if (existing == null) {
            RegistrationInsert insert = new RegistrationInsert(
                    name,
                    normalizedName,
                    request.gender(),
                    request.birthYear(),
                    phoneNumber,
                    phoneLastFour,
                    churchCellDepartment,
                    lookupKeyHash,
                    true,
                    false,
                    RegistrationStatus.REGISTERED
            );
            registrationMapper.insert(insert);
            Registration created = registrationMapper.findById(insert.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
            insertHistory(created.id(), RegistrationHistoryChangeType.CREATED, null, snapshot(created));
            return new RegistrationCreateResponse(
                    ResultType.CREATED,
                    RegistrationResponse.from(created),
                    lookupKey,
                    LOOKUP_KEY_NOTICE
            );
        }

        String previousSnapshot = snapshot(existing);
        registrationMapper.overwrite(new RegistrationOverwrite(
                existing.id(),
                name,
                normalizedName,
                request.gender(),
                request.birthYear(),
                phoneNumber,
                phoneLastFour,
                churchCellDepartment,
                lookupKeyHash,
                true
        ));
        Registration updated = registrationMapper.findById(existing.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        insertHistory(updated.id(), RegistrationHistoryChangeType.OVERWRITTEN, previousSnapshot, snapshot(updated));
        return new RegistrationCreateResponse(
                ResultType.OVERWRITTEN,
                RegistrationResponse.from(updated),
                lookupKey,
                LOOKUP_KEY_NOTICE
        );
    }

    @Transactional(readOnly = true)
    public RegistrationResponse selfLookup(RegistrationSelfLookupRequest request) {
        return RegistrationResponse.from(authenticateParticipant(request.name(), request.phoneLastFour(), request.lookupKey()));
    }

    @Transactional
    public RegistrationResponse selfUpdate(RegistrationSelfUpdateRequest request) {
        if (!registrationProperties.selfEditEnabled()) {
            throw new BusinessException(ErrorCode.REGISTRATION_EDIT_CLOSED);
        }

        Registration registration = authenticateParticipant(request.name(), request.phoneLastFour(), request.lookupKey());
        String newPhoneNumber = PhoneNumberNormalizer.normalize(request.update().phoneNumber());
        String newPhoneLastFour = PhoneNumberNormalizer.lastFour(newPhoneNumber);
        String normalizedName = normalizeName(registration.name());
        Registration conflict = registrationMapper
                .findActiveByNormalizedNameAndPhoneNumber(normalizedName, newPhoneNumber)
                .orElse(null);

        if (conflict != null && !conflict.id().equals(registration.id())) {
            throw new BusinessException(ErrorCode.DUPLICATE_REGISTRATION);
        }

        String previousSnapshot = snapshot(registration);
        registrationMapper.selfUpdate(new RegistrationSelfUpdate(
                registration.id(),
                request.update().gender(),
                request.update().birthYear(),
                newPhoneNumber,
                newPhoneLastFour,
                normalizeOptional(request.update().churchCellDepartment())
        ));
        Registration updated = registrationMapper.findById(registration.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        insertHistory(updated.id(), RegistrationHistoryChangeType.SELF_UPDATED, previousSnapshot, snapshot(updated));
        return RegistrationResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminRegistrationResponse> findRegistrations(AdminPrincipal admin, int page, int size) {
        requireRole(admin, AdminRole.STAFF);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<AdminRegistrationResponse> content = registrationMapper.findPage(safeSize, safePage * safeSize)
                .stream()
                .map(AdminRegistrationResponse::listItem)
                .toList();
        return PageResponse.of(content, safePage, safeSize, registrationMapper.countAll());
    }

    @Transactional
    public AdminRegistrationResponse findRegistration(AdminPrincipal admin, Long id) {
        requireRole(admin, AdminRole.STAFF);
        Registration registration = registrationMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGISTRATION_NOT_FOUND));
        insertPrivacyAccessLog(registration.id(), admin.id(), DETAIL_VIEW, "phone_number");
        return AdminRegistrationResponse.detail(registration);
    }

    @Transactional
    public List<RegistrationHistoryResponse> findHistories(AdminPrincipal admin, Long registrationId) {
        requireRole(admin, AdminRole.STAFF);
        if (registrationMapper.findById(registrationId).isEmpty()) {
            throw new BusinessException(ErrorCode.REGISTRATION_NOT_FOUND);
        }
        insertPrivacyAccessLog(registrationId, admin.id(), HISTORY_VIEW, "history_snapshots");
        return registrationHistoryMapper.findByRegistrationId(registrationId)
                .stream()
                .map(RegistrationHistoryResponse::from)
                .toList();
    }

    @Transactional
    public AdminRegistrationResponse updateFeePaid(
            AdminPrincipal admin,
            Long id,
            AdminRegistrationFeePaidUpdateRequest request
    ) {
        requireRole(admin, AdminRole.CHAIR);
        Registration registration = registrationMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGISTRATION_NOT_FOUND));
        String previousSnapshot = snapshot(registration);
        registrationMapper.updateFeePaid(id, request.feePaid());
        Registration updated = registrationMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        insertAdminHistory(
                updated.id(),
                RegistrationHistoryChangeType.FEE_PAYMENT_UPDATED,
                previousSnapshot,
                snapshot(updated),
                admin.id()
        );
        return AdminRegistrationResponse.detail(updated);
    }

    @Transactional
    public AdminRegistrationResponse updateStatus(
            AdminPrincipal admin,
            Long id,
            AdminRegistrationStatusUpdateRequest request
    ) {
        requireRole(admin, AdminRole.CHAIR);
        Registration registration = registrationMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGISTRATION_NOT_FOUND));
        String previousSnapshot = snapshot(registration);
        registrationMapper.updateStatus(id, request.status());
        Registration updated = registrationMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        insertAdminHistory(
                updated.id(),
                RegistrationHistoryChangeType.STATUS_UPDATED,
                previousSnapshot,
                snapshot(updated),
                admin.id()
        );
        return AdminRegistrationResponse.detail(updated);
    }

    @Transactional
    public AdminRegistrationResponse updateManagement(
            AdminPrincipal admin,
            Long id,
            AdminRegistrationManagementUpdateRequest request
    ) {
        requireRole(admin, AdminRole.CHAIR);
        Registration registration = registrationMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGISTRATION_NOT_FOUND));
        String previousSnapshot = snapshot(registration);
        registrationMapper.updateManagement(new RegistrationManagementUpdate(
                id,
                normalizeOptional(request.adminMemo()),
                request.newcomer(),
                request.careTarget()
        ));
        Registration updated = registrationMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        insertAdminHistory(
                updated.id(),
                RegistrationHistoryChangeType.ADMIN_MANAGEMENT_UPDATED,
                previousSnapshot,
                snapshot(updated),
                admin.id()
        );
        return AdminRegistrationResponse.detail(updated);
    }

    @Transactional
    public AdminRegistrationResponse updateParticipantChurchCell(
            AdminPrincipal admin,
            Long participantId,
            AdminParticipantChurchCellUpdateRequest request
    ) {
        requireRole(admin, AdminRole.CHAIR);
        Registration registration = registrationMapper.findById(participantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGISTRATION_NOT_FOUND));
        if (request.churchCellId() != null) {
            communityService.ensureCellExists(request.churchCellId());
        }

        String previousSnapshot = snapshot(registration);
        registrationMapper.updateChurchCell(participantId, request.churchCellId());
        Registration updated = registrationMapper.findById(participantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        insertAdminHistory(
                updated.id(),
                RegistrationHistoryChangeType.CHURCH_CELL_UPDATED,
                previousSnapshot,
                snapshot(updated),
                admin.id()
        );
        return AdminRegistrationResponse.detail(updated);
    }

    private Registration authenticateParticipant(String name, String phoneLastFour, String lookupKey) {
        String normalizedName = normalizeName(name);
        return registrationMapper.findActiveByNormalizedNameAndPhoneLastFour(normalizedName, phoneLastFour)
                .stream()
                .filter(registration -> passwordEncoder.matches(lookupKey, registration.lookupKeyHash()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.REGISTRATION_LOOKUP_FAILED));
    }

    private void insertHistory(
            Long registrationId,
            RegistrationHistoryChangeType changeType,
            String previousSnapshot,
            String newSnapshot
    ) {
        registrationHistoryMapper.insert(new RegistrationHistoryInsert(
                registrationId,
                changeType,
                previousSnapshot,
                newSnapshot,
                RegistrationActorType.PARTICIPANT,
                null
        ));
    }

    private void insertAdminHistory(
            Long registrationId,
            RegistrationHistoryChangeType changeType,
            String previousSnapshot,
            String newSnapshot,
            Long adminUserId
    ) {
        registrationHistoryMapper.insert(new RegistrationHistoryInsert(
                registrationId,
                changeType,
                previousSnapshot,
                newSnapshot,
                RegistrationActorType.ADMIN,
                adminUserId
        ));
    }

    private void insertPrivacyAccessLog(
            Long registrationId,
            Long adminUserId,
            String accessType,
            String sensitiveFields
    ) {
        privacyAccessLogMapper.insert(new RegistrationPrivacyAccessLogInsert(
                registrationId,
                adminUserId,
                accessType,
                sensitiveFields
        ));
    }

    private String snapshot(Registration registration) {
        try {
            Map<String, Object> snapshot = Map.ofEntries(
                    Map.entry("id", registration.id()),
                    Map.entry("name", registration.name()),
                    Map.entry("gender", registration.gender()),
                    Map.entry("birthYear", registration.birthYear()),
                    Map.entry("phoneNumber", registration.phoneNumber()),
                    Map.entry("phoneLastFour", registration.phoneLastFour()),
                    Map.entry("churchCellDepartment", registration.churchCellDepartment() == null
                            ? "" : registration.churchCellDepartment()),
                    Map.entry("churchCellId", registration.churchCellId() == null ? "" : registration.churchCellId()),
                    Map.entry("churchCellName", registration.churchCellName() == null ? "" : registration.churchCellName()),
                    Map.entry("middleGroupId", registration.middleGroupId() == null ? "" : registration.middleGroupId()),
                    Map.entry("middleGroupName", registration.middleGroupName() == null ? "" : registration.middleGroupName()),
                    Map.entry("retreatGroupId", registration.retreatGroupId() == null ? "" : registration.retreatGroupId()),
                    Map.entry("retreatGroupName", registration.retreatGroupName() == null ? "" : registration.retreatGroupName()),
                    Map.entry("retreatGroupLeader", registration.retreatGroupLeader() == null
                            ? "" : registration.retreatGroupLeader()),
                    Map.entry("privacyConsentAgreed", registration.privacyConsentAgreed()),
                    Map.entry("feePaid", registration.feePaid()),
                    Map.entry("status", registration.status()),
                    Map.entry("adminMemo", registration.adminMemo() == null ? "" : registration.adminMemo()),
                    Map.entry("newcomer", registration.newcomer()),
                    Map.entry("careTarget", registration.careTarget())
            );
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create registration snapshot.", exception);
        }
    }

    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        String normalized = name.trim();
        if (normalized.length() < 2 || normalized.length() > 50) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return normalized;
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
