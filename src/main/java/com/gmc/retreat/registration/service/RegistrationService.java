package com.gmc.retreat.registration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.community.service.CommunityService;
import com.gmc.retreat.error.BusinessException;
import com.gmc.retreat.error.ErrorCode;
import com.gmc.retreat.fee.mapper.FeeEventInsert;
import com.gmc.retreat.fee.mapper.FeeMapper;
import com.gmc.retreat.registration.domain.AttendanceType;
import com.gmc.retreat.registration.domain.Registration;
import com.gmc.retreat.registration.domain.RegistrationActorType;
import com.gmc.retreat.registration.domain.RegistrationHistoryChangeType;
import com.gmc.retreat.registration.domain.RegistrationStatus;
import com.gmc.retreat.registration.domain.TransportationMethod;
import com.gmc.retreat.registration.domain.WorshipBusRideSlot;
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
import com.gmc.retreat.registration.mapper.RegistrationHistoryInsert;
import com.gmc.retreat.registration.mapper.RegistrationHistoryMapper;
import com.gmc.retreat.registration.mapper.RegistrationInsert;
import com.gmc.retreat.registration.mapper.RegistrationManagementUpdate;
import com.gmc.retreat.registration.mapper.RegistrationMapper;
import com.gmc.retreat.registration.mapper.RegistrationOverwrite;
import com.gmc.retreat.registration.mapper.RegistrationSelfUpdate;
import com.gmc.retreat.registration.mapper.RegistrationPrivacyAccessLogInsert;
import com.gmc.retreat.registration.mapper.RegistrationPrivacyAccessLogMapper;
import com.gmc.retreat.security.auth.AdminPrincipal;
import java.time.OffsetDateTime;
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
                request.plannedArrivalAt(),
                request.plannedDepartureAt(),
                request.inboundTransportationMethod(),
                request.inboundCarpoolAvailable(),
                request.inboundCarpoolSeats(),
                request.inboundCarpoolArea(),
                request.inboundCarpoolRouteArea(),
                request.inboundCarpoolNote(),
                request.inboundCarpoolPreferredArea(),
                request.inboundCarpoolPreferredNote(),
                request.inboundWorshipBusRideSlot(),
                request.outboundTransportationMethod(),
                request.outboundCarpoolAvailable(),
                request.outboundCarpoolSeats(),
                request.outboundCarpoolArea(),
                request.outboundCarpoolRouteArea(),
                request.outboundCarpoolNote(),
                request.outboundCarpoolPreferredArea(),
                request.outboundCarpoolPreferredNote(),
                request.outboundWorshipBusRideSlot()
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
                    request.plannedArrivalAt(),
                    request.plannedDepartureAt(),
                    normalizeOptional(request.partialAttendanceNote()),
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
                    request.inboundCarpoolRouteArea(),
                    request.inboundCarpoolNote(),
                    request.inboundCarpoolPreferredArea(),
                    request.inboundCarpoolPreferredNote(),
                    request.inboundWorshipBusRideSlot(),
                    request.outboundTransportationMethod(),
                    request.outboundCarpoolAvailable(),
                    request.outboundCarpoolSeats(),
                    request.outboundCarpoolArea(),
                    request.outboundCarpoolRouteArea(),
                    request.outboundCarpoolNote(),
                    request.outboundCarpoolPreferredArea(),
                    request.outboundCarpoolPreferredNote(),
                    request.outboundWorshipBusRideSlot()
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
                request.plannedArrivalAt(),
                request.plannedDepartureAt(),
                normalizeOptional(request.partialAttendanceNote()),
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
                request.inboundCarpoolRouteArea(),
                request.inboundCarpoolNote(),
                request.inboundCarpoolPreferredArea(),
                request.inboundCarpoolPreferredNote(),
                request.inboundWorshipBusRideSlot(),
                request.outboundTransportationMethod(),
                request.outboundCarpoolAvailable(),
                request.outboundCarpoolSeats(),
                request.outboundCarpoolArea(),
                request.outboundCarpoolRouteArea(),
                request.outboundCarpoolNote(),
                request.outboundCarpoolPreferredArea(),
                request.outboundCarpoolPreferredNote(),
                request.outboundWorshipBusRideSlot()
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
                request.update().plannedArrivalAt(),
                request.update().plannedDepartureAt(),
                request.update().inboundTransportationMethod(),
                request.update().inboundCarpoolAvailable(),
                request.update().inboundCarpoolSeats(),
                request.update().inboundCarpoolArea(),
                request.update().inboundCarpoolRouteArea(),
                request.update().inboundCarpoolNote(),
                request.update().inboundCarpoolPreferredArea(),
                request.update().inboundCarpoolPreferredNote(),
                request.update().inboundWorshipBusRideSlot(),
                request.update().outboundTransportationMethod(),
                request.update().outboundCarpoolAvailable(),
                request.update().outboundCarpoolSeats(),
                request.update().outboundCarpoolArea(),
                request.update().outboundCarpoolRouteArea(),
                request.update().outboundCarpoolNote(),
                request.update().outboundCarpoolPreferredArea(),
                request.update().outboundCarpoolPreferredNote(),
                request.update().outboundWorshipBusRideSlot()
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
                request.update().plannedArrivalAt(),
                request.update().plannedDepartureAt(),
                normalizeOptional(request.update().partialAttendanceNote()),
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
                request.update().inboundCarpoolRouteArea(),
                request.update().inboundCarpoolNote(),
                request.update().inboundCarpoolPreferredArea(),
                request.update().inboundCarpoolPreferredNote(),
                request.update().inboundWorshipBusRideSlot(),
                request.update().outboundTransportationMethod(),
                request.update().outboundCarpoolAvailable(),
                request.update().outboundCarpoolSeats(),
                request.update().outboundCarpoolArea(),
                request.update().outboundCarpoolRouteArea(),
                request.update().outboundCarpoolNote(),
                request.update().outboundCarpoolPreferredArea(),
                request.update().outboundCarpoolPreferredNote(),
                request.update().outboundWorshipBusRideSlot()
        ));
        Registration updated = registrationMapper.findById(registration.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        insertHistory(updated.id(), RegistrationHistoryChangeType.SELF_UPDATED, previousSnapshot, snapshot(updated));
        return RegistrationResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminRegistrationResponse> findRegistrations(
            AdminPrincipal admin,
            String keyword,
            RegistrationStatus status,
            Boolean feePaid,
            Boolean newcomer,
            Boolean careTarget,
            Boolean checkedIn,
            Boolean retreatGroupAssigned,
            Boolean churchCellAssigned,
            AttendanceType attendanceType,
            String transportationNeed,
            String sort,
            int page,
            int size
    ) {
        requireRole(admin, AdminRole.STAFF);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String normalizedKeyword = normalizeOptional(keyword);
        String normalizedTransportationNeed = normalizeTransportationNeed(transportationNeed);
        String normalizedSort = normalizeRegistrationListSort(sort);
        List<AdminRegistrationResponse> content = registrationMapper.findPage(
                        normalizedKeyword,
                        status,
                        feePaid,
                        newcomer,
                        careTarget,
                        checkedIn,
                        retreatGroupAssigned,
                        churchCellAssigned,
                        attendanceType,
                        normalizedTransportationNeed,
                        normalizedSort,
                        safeSize,
                        safePage * safeSize
                )
                .stream()
                .map(AdminRegistrationResponse::listItem)
                .toList();
        long totalElements = registrationMapper.countAll(
                normalizedKeyword,
                status,
                feePaid,
                newcomer,
                careTarget,
                checkedIn,
                retreatGroupAssigned,
                churchCellAssigned,
                attendanceType,
                normalizedTransportationNeed
        );
        return PageResponse.of(content, safePage, safeSize, totalElements);
    }

    @Transactional
    public AdminRegistrationResponse findRegistration(AdminPrincipal admin, Long id) {
        requireRole(admin, AdminRole.STAFF);
        Registration registration = registrationMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGISTRATION_NOT_FOUND));
        insertPrivacyAccessLog(
                registration.id(),
                admin.id(),
                DETAIL_VIEW,
                "phone_number,transportation_carpool_fields"
        );
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
            OffsetDateTime plannedArrivalAt,
            OffsetDateTime plannedDepartureAt,
            TransportationMethod inboundTransportation,
            Boolean inboundCarpoolAvailable,
            Integer inboundCarpoolSeats,
            String inboundCarpoolArea,
            String inboundCarpoolRouteArea,
            String inboundCarpoolNote,
            String inboundCarpoolPreferredArea,
            String inboundCarpoolPreferredNote,
            WorshipBusRideSlot inboundWorshipBusRideSlot,
            TransportationMethod outboundTransportation,
            Boolean outboundCarpoolAvailable,
            Integer outboundCarpoolSeats,
            String outboundCarpoolArea,
            String outboundCarpoolRouteArea,
            String outboundCarpoolNote,
            String outboundCarpoolPreferredArea,
            String outboundCarpoolPreferredNote,
            WorshipBusRideSlot outboundWorshipBusRideSlot
    ) {
        validateAttendanceTimeRange(attendanceType, plannedArrivalAt, plannedDepartureAt);
        validateTransportationMethod(attendanceType, inboundTransportation, outboundTransportation);
        validateWorshipBusSlot(inboundTransportation, inboundWorshipBusRideSlot, true);
        validateWorshipBusSlot(outboundTransportation, outboundWorshipBusRideSlot, false);
        validateDirectionalCarpool(
                inboundTransportation, inboundCarpoolAvailable, inboundCarpoolSeats,
                inboundCarpoolArea, inboundCarpoolRouteArea, inboundCarpoolNote,
                inboundCarpoolPreferredArea, inboundCarpoolPreferredNote
        );
        validateDirectionalCarpool(
                outboundTransportation, outboundCarpoolAvailable, outboundCarpoolSeats,
                outboundCarpoolArea, outboundCarpoolRouteArea, outboundCarpoolNote,
                outboundCarpoolPreferredArea, outboundCarpoolPreferredNote
        );
    }

    private void validateAttendanceTimeRange(
            AttendanceType attendanceType,
            OffsetDateTime plannedArrivalAt,
            OffsetDateTime plannedDepartureAt
    ) {
        if (attendanceType == AttendanceType.PARTIAL) {
            if (plannedArrivalAt == null || plannedDepartureAt == null
                    || !plannedDepartureAt.isAfter(plannedArrivalAt)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            return;
        }

        if (plannedArrivalAt != null || plannedDepartureAt != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateTransportationMethod(
            AttendanceType attendanceType,
            TransportationMethod inboundTransportation,
            TransportationMethod outboundTransportation
    ) {
        if (!isAllowedTransportationMethod(attendanceType, inboundTransportation)
                || !isAllowedTransportationMethod(attendanceType, outboundTransportation)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        if ((inboundTransportation == TransportationMethod.OWN_CAR)
                != (outboundTransportation == TransportationMethod.OWN_CAR)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private boolean isAllowedTransportationMethod(
            AttendanceType attendanceType,
            TransportationMethod transportationMethod
    ) {
        if (attendanceType == AttendanceType.FULL) {
            return transportationMethod == TransportationMethod.OWN_CAR
                    || transportationMethod == TransportationMethod.GROUP_BUS
                    || transportationMethod == TransportationMethod.PUBLIC_TRANSIT
                    || transportationMethod == TransportationMethod.CARPOOL_NEEDED;
        }

        if (attendanceType == AttendanceType.WORSHIP_ONLY) {
            return transportationMethod == TransportationMethod.OWN_CAR
                    || transportationMethod == TransportationMethod.WORSHIP_SHUTTLE
                    || transportationMethod == TransportationMethod.PUBLIC_TRANSIT
                    || transportationMethod == TransportationMethod.CARPOOL_NEEDED
                    || transportationMethod == TransportationMethod.NOT_DECIDED;
        }

        return transportationMethod == TransportationMethod.OWN_CAR
                || transportationMethod == TransportationMethod.GROUP_BUS
                || transportationMethod == TransportationMethod.WORSHIP_SHUTTLE
                || transportationMethod == TransportationMethod.PUBLIC_TRANSIT
                || transportationMethod == TransportationMethod.CARPOOL_NEEDED
                || transportationMethod == TransportationMethod.NOT_DECIDED;
    }

    private void validateWorshipBusSlot(
            TransportationMethod transportationMethod,
            WorshipBusRideSlot worshipBusRideSlot,
            boolean inbound
    ) {
        if (transportationMethod != TransportationMethod.WORSHIP_SHUTTLE) {
            if (worshipBusRideSlot != null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            return;
        }

        if (worshipBusRideSlot == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        boolean inboundSlot = worshipBusRideSlot == WorshipBusRideSlot.DAY1_BEFORE_WORSHIP
                || worshipBusRideSlot == WorshipBusRideSlot.DAY2_BEFORE_WORSHIP;
        if (inbound != inboundSlot) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateDirectionalCarpool(
            TransportationMethod direction,
            Boolean carpoolAvailable,
            Integer carpoolSeats,
            String carpoolArea,
            String carpoolRouteArea,
            String carpoolNote,
            String carpoolPreferredArea,
            String carpoolPreferredNote
    ) {
        if (direction == TransportationMethod.OWN_CAR) {
            if (carpoolAvailable == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            if (Boolean.TRUE.equals(carpoolAvailable)) {
                if (carpoolSeats == null || carpoolSeats < 1 || !StringUtils.hasText(carpoolArea)) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST);
                }
            } else if (carpoolSeats != null
                    || carpoolArea != null
                    || carpoolRouteArea != null
                    || carpoolNote != null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            if (carpoolPreferredArea != null || carpoolPreferredNote != null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        } else if (direction == TransportationMethod.CARPOOL_NEEDED) {
            if (carpoolAvailable != null
                    || carpoolSeats != null
                    || carpoolArea != null
                    || carpoolRouteArea != null
                    || carpoolNote != null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            if (!StringUtils.hasText(carpoolPreferredArea)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        } else if (carpoolAvailable != null
                || carpoolSeats != null
                || carpoolArea != null
                || carpoolRouteArea != null
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
                    Map.entry("plannedArrivalAt", registration.plannedArrivalAt() == null
                            ? "" : registration.plannedArrivalAt()),
                    Map.entry("plannedDepartureAt", registration.plannedDepartureAt() == null
                            ? "" : registration.plannedDepartureAt()),
                    Map.entry("partialAttendanceNote", registration.partialAttendanceNote() == null
                            ? "" : registration.partialAttendanceNote()),
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
                    Map.entry("inboundCarpoolRouteArea", registration.inboundCarpoolRouteArea() == null
                            ? "" : registration.inboundCarpoolRouteArea()),
                    Map.entry("inboundCarpoolNote", registration.inboundCarpoolNote() == null
                            ? "" : registration.inboundCarpoolNote()),
                    Map.entry("inboundCarpoolPreferredArea", registration.inboundCarpoolPreferredArea() == null
                            ? "" : registration.inboundCarpoolPreferredArea()),
                    Map.entry("inboundCarpoolPreferredNote", registration.inboundCarpoolPreferredNote() == null
                            ? "" : registration.inboundCarpoolPreferredNote()),
                    Map.entry("inboundWorshipBusRideSlot", registration.inboundWorshipBusRideSlot() == null
                            ? "" : registration.inboundWorshipBusRideSlot()),
                    Map.entry("outboundTransportationMethod", registration.outboundTransportationMethod()),
                    Map.entry("outboundCarpoolAvailable", registration.outboundCarpoolAvailable() == null
                            ? "" : registration.outboundCarpoolAvailable()),
                    Map.entry("outboundCarpoolSeats", registration.outboundCarpoolSeats() == null
                            ? "" : registration.outboundCarpoolSeats()),
                    Map.entry("outboundCarpoolArea", registration.outboundCarpoolArea() == null
                            ? "" : registration.outboundCarpoolArea()),
                    Map.entry("outboundCarpoolRouteArea", registration.outboundCarpoolRouteArea() == null
                            ? "" : registration.outboundCarpoolRouteArea()),
                    Map.entry("outboundCarpoolNote", registration.outboundCarpoolNote() == null
                            ? "" : registration.outboundCarpoolNote()),
                    Map.entry("outboundCarpoolPreferredArea", registration.outboundCarpoolPreferredArea() == null
                            ? "" : registration.outboundCarpoolPreferredArea()),
                    Map.entry("outboundCarpoolPreferredNote", registration.outboundCarpoolPreferredNote() == null
                            ? "" : registration.outboundCarpoolPreferredNote()),
                    Map.entry("outboundWorshipBusRideSlot", registration.outboundWorshipBusRideSlot() == null
                            ? "" : registration.outboundWorshipBusRideSlot())
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

    private String normalizeTransportationNeed(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        if ("CARPOOL_NEEDED".equals(normalized) || "CARPOOL_AVAILABLE".equals(normalized)) {
            return normalized;
        }
        throw new BusinessException(ErrorCode.INVALID_REQUEST);
    }

    private String normalizeRegistrationListSort(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return "created_desc";
        }
        return switch (normalized) {
            case "created_desc", "name_asc", "fee_unpaid_first", "check_in_pending_first", "group_asc" -> normalized;
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST);
        };
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
