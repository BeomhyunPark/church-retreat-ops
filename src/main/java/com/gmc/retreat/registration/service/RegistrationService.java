package com.gmc.retreat.registration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.community.service.CommunityService;
import com.gmc.retreat.error.BusinessException;
import com.gmc.retreat.error.ErrorCode;
import com.gmc.retreat.fee.mapper.FeeMapper;
import com.gmc.retreat.fee.mapper.FeeMapper.FeeEventInsert;
import com.gmc.retreat.registration.domain.AttendanceType;
import com.gmc.retreat.registration.domain.Registration;
import com.gmc.retreat.registration.domain.RegistrationActorType;
import com.gmc.retreat.registration.domain.RegistrationHistoryChangeType;
import com.gmc.retreat.registration.domain.RegistrationStatus;
import com.gmc.retreat.registration.domain.TransportationMethod;
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

    private static final String DETAIL_VIEW = "DETAIL_VIEW";
    private static final String HISTORY_VIEW = "HISTORY_VIEW";

    private final RegistrationMapper registrationMapper;
    private final RegistrationHistoryMapper registrationHistoryMapper;
    private final RegistrationPrivacyAccessLogMapper privacyAccessLogMapper;
    private final FeeMapper feeMapper;
    private final CommunityService communityService;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationProperties registrationProperties;
    private final ObjectMapper objectMapper;

    public RegistrationService(
            RegistrationMapper registrationMapper,
            RegistrationHistoryMapper registrationHistoryMapper,
            RegistrationPrivacyAccessLogMapper privacyAccessLogMapper,
            FeeMapper feeMapper,
            CommunityService communityService,
            PasswordEncoder passwordEncoder,
            RegistrationProperties registrationProperties,
            ObjectMapper objectMapper
    ) {
        this.registrationMapper = registrationMapper;
        this.registrationHistoryMapper = registrationHistoryMapper;
        this.privacyAccessLogMapper = privacyAccessLogMapper;
        this.feeMapper = feeMapper;
        this.communityService = communityService;
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
        String lookupKeyHash = passwordEncoder.encode(request.lookupKey());
        String churchCellDepartment = normalizeOptional(request.churchCellDepartment());
        validateAttendanceSurvey(
                request.attendanceType(),
                request.inboundTransportationMethod(),
                request.inboundCarpoolAvailable(),
                request.inboundCarpoolSeats(),
                request.inboundCarpoolArea(),
                request.inboundCarpoolNote(),
                request.inboundCarpoolPreferredArea(),
                request.inboundCarpoolPreferredNote(),
                request.outboundTransportationMethod(),
                request.outboundCarpoolAvailable(),
                request.outboundCarpoolSeats(),
                request.outboundCarpoolArea(),
                request.outboundCarpoolNote(),
                request.outboundCarpoolPreferredArea(),
                request.outboundCarpoolPreferredNote()
        );
        AttendanceFields attendanceFields = resolveAttendanceFields(request.attendanceType(), request);

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
                    RegistrationStatus.REGISTERED,
                    request.attendanceType(),
                    attendanceFields.lodgingNight1(),
                    attendanceFields.lodgingNight2(),
                    attendanceFields.attendDay1Morning(),
                    attendanceFields.attendDay1Afternoon(),
                    attendanceFields.attendDay1Worship(),
                    attendanceFields.attendDay2Morning(),
                    attendanceFields.attendDay2Afternoon(),
                    attendanceFields.attendDay2Worship(),
                    attendanceFields.attendDay3Morning(),
                    attendanceFields.attendDay3Afternoon(),
                    request.inboundTransportationMethod(),
                    request.inboundCarpoolAvailable(),
                    request.inboundCarpoolSeats(),
                    request.inboundCarpoolArea(),
                    request.inboundCarpoolNote(),
                    request.inboundCarpoolPreferredArea(),
                    request.inboundCarpoolPreferredNote(),
                    request.outboundTransportationMethod(),
                    request.outboundCarpoolAvailable(),
                    request.outboundCarpoolSeats(),
                    request.outboundCarpoolArea(),
                    request.outboundCarpoolNote(),
                    request.outboundCarpoolPreferredArea(),
                    request.outboundCarpoolPreferredNote()
            );
            registrationMapper.insert(insert);
            Registration created = registrationMapper.findById(insert.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
            insertHistory(created.id(), RegistrationHistoryChangeType.CREATED, null, snapshot(created));
            return new RegistrationCreateResponse(
                    ResultType.CREATED,
                    RegistrationResponse.from(created)
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
                true,
                request.attendanceType(),
                attendanceFields.lodgingNight1(),
                attendanceFields.lodgingNight2(),
                attendanceFields.attendDay1Morning(),
                attendanceFields.attendDay1Afternoon(),
                attendanceFields.attendDay1Worship(),
                attendanceFields.attendDay2Morning(),
                attendanceFields.attendDay2Afternoon(),
                attendanceFields.attendDay2Worship(),
                attendanceFields.attendDay3Morning(),
                attendanceFields.attendDay3Afternoon(),
                request.inboundTransportationMethod(),
                request.inboundCarpoolAvailable(),
                request.inboundCarpoolSeats(),
                request.inboundCarpoolArea(),
                request.inboundCarpoolNote(),
                request.inboundCarpoolPreferredArea(),
                request.inboundCarpoolPreferredNote(),
                request.outboundTransportationMethod(),
                request.outboundCarpoolAvailable(),
                request.outboundCarpoolSeats(),
                request.outboundCarpoolArea(),
                request.outboundCarpoolNote(),
                request.outboundCarpoolPreferredArea(),
                request.outboundCarpoolPreferredNote()
        ));
        Registration updated = registrationMapper.findById(existing.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        insertHistory(updated.id(), RegistrationHistoryChangeType.OVERWRITTEN, previousSnapshot, snapshot(updated));
        return new RegistrationCreateResponse(
                ResultType.OVERWRITTEN,
                RegistrationResponse.from(updated)
        );
    }

    @Transactional(readOnly = true)
    public RegistrationResponse selfLookup(RegistrationSelfLookupRequest request) {
        return RegistrationResponse.from(authenticateParticipantByName(request.name(), request.lookupKey()));
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

        validateAttendanceSurvey(
                request.update().attendanceType(),
                request.update().inboundTransportationMethod(),
                request.update().inboundCarpoolAvailable(),
                request.update().inboundCarpoolSeats(),
                request.update().inboundCarpoolArea(),
                request.update().inboundCarpoolNote(),
                request.update().inboundCarpoolPreferredArea(),
                request.update().inboundCarpoolPreferredNote(),
                request.update().outboundTransportationMethod(),
                request.update().outboundCarpoolAvailable(),
                request.update().outboundCarpoolSeats(),
                request.update().outboundCarpoolArea(),
                request.update().outboundCarpoolNote(),
                request.update().outboundCarpoolPreferredArea(),
                request.update().outboundCarpoolPreferredNote()
        );
        AttendanceFields attendanceFields = resolveAttendanceFields(request.update().attendanceType(), request.update());

        String previousSnapshot = snapshot(registration);
        registrationMapper.selfUpdate(new RegistrationSelfUpdate(
                registration.id(),
                request.update().gender(),
                request.update().birthYear(),
                newPhoneNumber,
                newPhoneLastFour,
                normalizeOptional(request.update().churchCellDepartment()),
                request.update().attendanceType(),
                attendanceFields.lodgingNight1(),
                attendanceFields.lodgingNight2(),
                attendanceFields.attendDay1Morning(),
                attendanceFields.attendDay1Afternoon(),
                attendanceFields.attendDay1Worship(),
                attendanceFields.attendDay2Morning(),
                attendanceFields.attendDay2Afternoon(),
                attendanceFields.attendDay2Worship(),
                attendanceFields.attendDay3Morning(),
                attendanceFields.attendDay3Afternoon(),
                request.update().inboundTransportationMethod(),
                request.update().inboundCarpoolAvailable(),
                request.update().inboundCarpoolSeats(),
                request.update().inboundCarpoolArea(),
                request.update().inboundCarpoolNote(),
                request.update().inboundCarpoolPreferredArea(),
                request.update().inboundCarpoolPreferredNote(),
                request.update().outboundTransportationMethod(),
                request.update().outboundCarpoolAvailable(),
                request.update().outboundCarpoolSeats(),
                request.update().outboundCarpoolArea(),
                request.update().outboundCarpoolNote(),
                request.update().outboundCarpoolPreferredArea(),
                request.update().outboundCarpoolPreferredNote()
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
        String reason = normalizeFeeReason(request.feePaid(), request.reason());
        String previousSnapshot = snapshot(registration);
        Boolean previousFeePaid = feeMapper.updateFeePaidIfChanged(id, request.feePaid(), admin.id())
                .orElseThrow(() -> noFeeChangeException(id, request.feePaid()));
        feeMapper.insertEvent(new FeeEventInsert(
                id,
                previousFeePaid,
                request.feePaid(),
                admin.id(),
                reason
        ));
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

    private Registration authenticateParticipantByName(String name, String lookupKey) {
        String normalizedName = normalizeName(name);
        return registrationMapper.findActiveByNormalizedName(normalizedName)
                .stream()
                .filter(registration -> passwordEncoder.matches(lookupKey, registration.lookupKeyHash()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.REGISTRATION_LOOKUP_FAILED));
    }

    private void validateAttendanceSurvey(
            AttendanceType attendanceType,
            TransportationMethod inboundTransportation,
            Boolean inboundCarpoolAvailable,
            Integer inboundCarpoolSeats,
            String inboundCarpoolArea,
            String inboundCarpoolNote,
            String inboundCarpoolPreferredArea,
            String inboundCarpoolPreferredNote,
            TransportationMethod outboundTransportation,
            Boolean outboundCarpoolAvailable,
            Integer outboundCarpoolSeats,
            String outboundCarpoolArea,
            String outboundCarpoolNote,
            String outboundCarpoolPreferredArea,
            String outboundCarpoolPreferredNote
    ) {
        validateFullAttendanceTransportation(attendanceType, inboundTransportation, outboundTransportation);
        validateDirectionalCarpool(
                inboundTransportation, inboundCarpoolAvailable, inboundCarpoolSeats,
                inboundCarpoolArea, inboundCarpoolNote, inboundCarpoolPreferredArea, inboundCarpoolPreferredNote
        );
        validateDirectionalCarpool(
                outboundTransportation, outboundCarpoolAvailable, outboundCarpoolSeats,
                outboundCarpoolArea, outboundCarpoolNote, outboundCarpoolPreferredArea, outboundCarpoolPreferredNote
        );
    }

    private void validateFullAttendanceTransportation(
            AttendanceType attendanceType,
            TransportationMethod inboundTransportation,
            TransportationMethod outboundTransportation
    ) {
        if (attendanceType != AttendanceType.FULL) {
            return;
        }

        if (!isFullAttendanceTransportationMethod(inboundTransportation)
                || !isFullAttendanceTransportationMethod(outboundTransportation)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if ((inboundTransportation == TransportationMethod.OWN_CAR)
                != (outboundTransportation == TransportationMethod.OWN_CAR)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private boolean isFullAttendanceTransportationMethod(TransportationMethod transportationMethod) {
        return transportationMethod == TransportationMethod.OWN_CAR
                || transportationMethod == TransportationMethod.GROUP_BUS
                || transportationMethod == TransportationMethod.PUBLIC_TRANSIT
                || transportationMethod == TransportationMethod.CARPOOL_NEEDED;
    }

    private void validateDirectionalCarpool(
            TransportationMethod direction,
            Boolean carpoolAvailable,
            Integer carpoolSeats,
            String carpoolArea,
            String carpoolNote,
            String carpoolPreferredArea,
            String carpoolPreferredNote
    ) {
        if (direction == TransportationMethod.OWN_CAR) {
            if (carpoolAvailable == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            if (Boolean.TRUE.equals(carpoolAvailable)) {
                if (carpoolSeats == null || !StringUtils.hasText(carpoolArea)) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST);
                }
            } else if (carpoolSeats != null || carpoolArea != null || carpoolNote != null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            if (carpoolPreferredArea != null || carpoolPreferredNote != null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        } else if (direction == TransportationMethod.CARPOOL_NEEDED) {
            if (carpoolAvailable != null || carpoolSeats != null || carpoolArea != null || carpoolNote != null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            if (!StringUtils.hasText(carpoolPreferredArea)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        } else if (carpoolAvailable != null
                || carpoolSeats != null
                || carpoolArea != null
                || carpoolNote != null
                || carpoolPreferredArea != null
                || carpoolPreferredNote != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private AttendanceFields resolveAttendanceFields(AttendanceType attendanceType, RegistrationCreateRequest request) {
        return resolveAttendanceFields(
                attendanceType,
                request.lodgingNight1(),
                request.lodgingNight2(),
                request.attendDay1Morning(),
                request.attendDay1Afternoon(),
                request.attendDay1Worship(),
                request.attendDay2Morning(),
                request.attendDay2Afternoon(),
                request.attendDay2Worship(),
                request.attendDay3Morning(),
                request.attendDay3Afternoon()
        );
    }

    private AttendanceFields resolveAttendanceFields(AttendanceType attendanceType, RegistrationSelfUpdateRequest.Update update) {
        return resolveAttendanceFields(
                attendanceType,
                update.lodgingNight1(),
                update.lodgingNight2(),
                update.attendDay1Morning(),
                update.attendDay1Afternoon(),
                update.attendDay1Worship(),
                update.attendDay2Morning(),
                update.attendDay2Afternoon(),
                update.attendDay2Worship(),
                update.attendDay3Morning(),
                update.attendDay3Afternoon()
        );
    }

    private AttendanceFields resolveAttendanceFields(
            AttendanceType attendanceType,
            Boolean lodgingNight1,
            Boolean lodgingNight2,
            Boolean attendDay1Morning,
            Boolean attendDay1Afternoon,
            Boolean attendDay1Worship,
            Boolean attendDay2Morning,
            Boolean attendDay2Afternoon,
            Boolean attendDay2Worship,
            Boolean attendDay3Morning,
            Boolean attendDay3Afternoon
    ) {
        if (attendanceType == AttendanceType.FULL) {
            return new AttendanceFields(true, true, true, true, true, true, true, true, true, true);
        }

        boolean resolvedLodgingNight1 = attendanceType == AttendanceType.WORSHIP_ONLY
                ? false : Boolean.TRUE.equals(lodgingNight1);
        boolean resolvedLodgingNight2 = attendanceType == AttendanceType.WORSHIP_ONLY
                ? false : Boolean.TRUE.equals(lodgingNight2);

        return new AttendanceFields(
                resolvedLodgingNight1,
                resolvedLodgingNight2,
                Boolean.TRUE.equals(attendDay1Morning),
                Boolean.TRUE.equals(attendDay1Afternoon),
                Boolean.TRUE.equals(attendDay1Worship),
                Boolean.TRUE.equals(attendDay2Morning),
                Boolean.TRUE.equals(attendDay2Afternoon),
                Boolean.TRUE.equals(attendDay2Worship),
                Boolean.TRUE.equals(attendDay3Morning),
                Boolean.TRUE.equals(attendDay3Afternoon)
        );
    }

    private record AttendanceFields(
            Boolean lodgingNight1,
            Boolean lodgingNight2,
            Boolean attendDay1Morning,
            Boolean attendDay1Afternoon,
            Boolean attendDay1Worship,
            Boolean attendDay2Morning,
            Boolean attendDay2Afternoon,
            Boolean attendDay2Worship,
            Boolean attendDay3Morning,
            Boolean attendDay3Afternoon
    ) {
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
            Map<String, Object> snapshot = Map.<String, Object>ofEntries(
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
                    Map.entry("careTarget", registration.careTarget()),
                    Map.entry("attendanceType", registration.attendanceType()),
                    Map.entry("lodgingNight1", registration.lodgingNight1()),
                    Map.entry("lodgingNight2", registration.lodgingNight2()),
                    Map.entry("attendDay1Morning", registration.attendDay1Morning()),
                    Map.entry("attendDay1Afternoon", registration.attendDay1Afternoon()),
                    Map.entry("attendDay1Worship", registration.attendDay1Worship()),
                    Map.entry("attendDay2Morning", registration.attendDay2Morning()),
                    Map.entry("attendDay2Afternoon", registration.attendDay2Afternoon()),
                    Map.entry("attendDay2Worship", registration.attendDay2Worship()),
                    Map.entry("attendDay3Morning", registration.attendDay3Morning()),
                    Map.entry("attendDay3Afternoon", registration.attendDay3Afternoon()),
                    Map.entry("inboundTransportationMethod", registration.inboundTransportationMethod()),
                    Map.entry("inboundCarpoolAvailable", registration.inboundCarpoolAvailable() == null
                            ? "" : registration.inboundCarpoolAvailable()),
                    Map.entry("inboundCarpoolSeats", registration.inboundCarpoolSeats() == null
                            ? "" : registration.inboundCarpoolSeats()),
                    Map.entry("inboundCarpoolArea", registration.inboundCarpoolArea() == null
                            ? "" : registration.inboundCarpoolArea()),
                    Map.entry("inboundCarpoolNote", registration.inboundCarpoolNote() == null
                            ? "" : registration.inboundCarpoolNote()),
                    Map.entry("inboundCarpoolPreferredArea", registration.inboundCarpoolPreferredArea() == null
                            ? "" : registration.inboundCarpoolPreferredArea()),
                    Map.entry("inboundCarpoolPreferredNote", registration.inboundCarpoolPreferredNote() == null
                            ? "" : registration.inboundCarpoolPreferredNote()),
                    Map.entry("outboundTransportationMethod", registration.outboundTransportationMethod()),
                    Map.entry("outboundCarpoolAvailable", registration.outboundCarpoolAvailable() == null
                            ? "" : registration.outboundCarpoolAvailable()),
                    Map.entry("outboundCarpoolSeats", registration.outboundCarpoolSeats() == null
                            ? "" : registration.outboundCarpoolSeats()),
                    Map.entry("outboundCarpoolArea", registration.outboundCarpoolArea() == null
                            ? "" : registration.outboundCarpoolArea()),
                    Map.entry("outboundCarpoolNote", registration.outboundCarpoolNote() == null
                            ? "" : registration.outboundCarpoolNote()),
                    Map.entry("outboundCarpoolPreferredArea", registration.outboundCarpoolPreferredArea() == null
                            ? "" : registration.outboundCarpoolPreferredArea()),
                    Map.entry("outboundCarpoolPreferredNote", registration.outboundCarpoolPreferredNote() == null
                            ? "" : registration.outboundCarpoolPreferredNote())
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

    private String normalizeFeeReason(Boolean feePaid, String value) {
        String reason = normalizeOptional(value);
        if (Boolean.FALSE.equals(feePaid) && reason == null) {
            throw new BusinessException(ErrorCode.FEE_REVERT_REASON_REQUIRED);
        }
        return reason;
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

    private void requireRole(AdminPrincipal admin, AdminRole requiredRole) {
        if (admin == null || !admin.role().hasAuthorityAtLeast(requiredRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
