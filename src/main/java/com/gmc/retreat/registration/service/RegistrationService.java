package com.gmc.retreat.registration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmc.retreat.error.BusinessException;
import com.gmc.retreat.error.ErrorCode;
import com.gmc.retreat.registration.domain.Registration;
import com.gmc.retreat.registration.domain.RegistrationActorType;
import com.gmc.retreat.registration.domain.RegistrationHistoryChangeType;
import com.gmc.retreat.registration.domain.RegistrationStatus;
import com.gmc.retreat.registration.dto.AdminRegistrationResponse;
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
import com.gmc.retreat.registration.mapper.RegistrationMapper.RegistrationOverwrite;
import com.gmc.retreat.registration.mapper.RegistrationMapper.RegistrationSelfUpdate;
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

    private final RegistrationMapper registrationMapper;
    private final RegistrationHistoryMapper registrationHistoryMapper;
    private final LookupKeyGenerator lookupKeyGenerator;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationProperties registrationProperties;
    private final ObjectMapper objectMapper;

    public RegistrationService(
            RegistrationMapper registrationMapper,
            RegistrationHistoryMapper registrationHistoryMapper,
            LookupKeyGenerator lookupKeyGenerator,
            PasswordEncoder passwordEncoder,
            RegistrationProperties registrationProperties,
            ObjectMapper objectMapper
    ) {
        this.registrationMapper = registrationMapper;
        this.registrationHistoryMapper = registrationHistoryMapper;
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
    public PageResponse<AdminRegistrationResponse> findRegistrations(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<AdminRegistrationResponse> content = registrationMapper.findPage(safeSize, safePage * safeSize)
                .stream()
                .map(AdminRegistrationResponse::listItem)
                .toList();
        return PageResponse.of(content, safePage, safeSize, registrationMapper.countAll());
    }

    @Transactional(readOnly = true)
    public AdminRegistrationResponse findRegistration(Long id) {
        Registration registration = registrationMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGISTRATION_NOT_FOUND));
        return AdminRegistrationResponse.detail(registration);
    }

    @Transactional(readOnly = true)
    public List<RegistrationHistoryResponse> findHistories(Long registrationId) {
        if (registrationMapper.findById(registrationId).isEmpty()) {
            throw new BusinessException(ErrorCode.REGISTRATION_NOT_FOUND);
        }
        return registrationHistoryMapper.findByRegistrationId(registrationId)
                .stream()
                .map(RegistrationHistoryResponse::from)
                .toList();
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
                    Map.entry("privacyConsentAgreed", registration.privacyConsentAgreed()),
                    Map.entry("feePaid", registration.feePaid()),
                    Map.entry("status", registration.status())
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
}
