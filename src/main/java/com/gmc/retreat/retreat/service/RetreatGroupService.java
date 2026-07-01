package com.gmc.retreat.retreat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.community.dto.ActiveUpdateRequest;
import com.gmc.retreat.error.BusinessException;
import com.gmc.retreat.error.ErrorCode;
import com.gmc.retreat.registration.domain.Registration;
import com.gmc.retreat.registration.domain.RegistrationActorType;
import com.gmc.retreat.registration.domain.RegistrationHistoryChangeType;
import com.gmc.retreat.registration.dto.AdminRegistrationResponse;
import com.gmc.retreat.registration.mapper.RegistrationHistoryInsert;
import com.gmc.retreat.registration.mapper.RegistrationHistoryMapper;
import com.gmc.retreat.registration.mapper.RegistrationMapper;
import com.gmc.retreat.retreat.domain.RetreatGroup;
import com.gmc.retreat.retreat.domain.RetreatGroupMember;
import com.gmc.retreat.retreat.dto.RetreatGroupAssignmentRequest;
import com.gmc.retreat.retreat.dto.RetreatGroupLeaderRequest;
import com.gmc.retreat.retreat.dto.RetreatGroupMemberResponse;
import com.gmc.retreat.retreat.dto.RetreatGroupMemberOrderRequest;
import com.gmc.retreat.retreat.dto.RetreatGroupRequest;
import com.gmc.retreat.retreat.dto.RetreatGroupResponse;
import com.gmc.retreat.retreat.dto.RetreatGroupTreeResponse;
import com.gmc.retreat.retreat.dto.RetreatGroupTreeResponse.GroupNode;
import com.gmc.retreat.retreat.dto.RetreatGroupTreeResponse.MemberNode;
import com.gmc.retreat.retreat.mapper.RetreatGroupMapper;
import com.gmc.retreat.retreat.mapper.RetreatGroupUpsert;
import com.gmc.retreat.security.auth.AdminPrincipal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RetreatGroupService {

    private final RetreatGroupMapper retreatGroupMapper;
    private final RegistrationMapper registrationMapper;
    private final RegistrationHistoryMapper registrationHistoryMapper;
    private final ObjectMapper objectMapper;

    public RetreatGroupService(
            RetreatGroupMapper retreatGroupMapper,
            RegistrationMapper registrationMapper,
            RegistrationHistoryMapper registrationHistoryMapper,
            ObjectMapper objectMapper
    ) {
        this.retreatGroupMapper = retreatGroupMapper;
        this.registrationMapper = registrationMapper;
        this.registrationHistoryMapper = registrationHistoryMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<RetreatGroupResponse> findGroups(AdminPrincipal admin) {
        requireRole(admin, AdminRole.STAFF);
        return retreatGroupMapper.findGroups()
                .stream()
                .map(RetreatGroupResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RetreatGroupResponse findGroup(AdminPrincipal admin, Long id) {
        requireRole(admin, AdminRole.STAFF);
        return RetreatGroupResponse.from(findGroupOrThrow(id));
    }

    @Transactional
    public RetreatGroupResponse createGroup(AdminPrincipal admin, RetreatGroupRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        String name = normalizeRequired(request.name());
        ensureGroupNameAvailable(null, name);
        RetreatGroupUpsert insert = new RetreatGroupUpsert(
                null,
                name,
                normalizeOptional(request.description()),
                request.displayOrder()
        );
        retreatGroupMapper.insertGroup(insert);
        return RetreatGroupResponse.from(findGroupOrThrow(insert.getId()));
    }

    @Transactional
    public RetreatGroupResponse updateGroup(AdminPrincipal admin, Long id, RetreatGroupRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        findGroupOrThrow(id);
        String name = normalizeRequired(request.name());
        ensureGroupNameAvailable(id, name);
        retreatGroupMapper.updateGroup(new RetreatGroupUpsert(
                id,
                name,
                normalizeOptional(request.description()),
                request.displayOrder()
        ));
        return RetreatGroupResponse.from(findGroupOrThrow(id));
    }

    @Transactional
    public RetreatGroupResponse updateGroupActive(AdminPrincipal admin, Long id, ActiveUpdateRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        findGroupOrThrow(id);
        retreatGroupMapper.updateGroupActive(id, request.active());
        return RetreatGroupResponse.from(findGroupOrThrow(id));
    }

    @Transactional
    public void deleteGroup(AdminPrincipal admin, Long id) {
        requireRole(admin, AdminRole.CHAIR);
        findGroupOrThrow(id);
        retreatGroupMapper.deleteMembersByGroupId(id);
        retreatGroupMapper.deleteGroupById(id);
    }

    @Transactional(readOnly = true)
    public List<RetreatGroupMemberResponse> findMembers(AdminPrincipal admin, Long groupId) {
        requireRole(admin, AdminRole.STAFF);
        findGroupOrThrow(groupId);
        return retreatGroupMapper.findMembersByGroupId(groupId)
                .stream()
                .map(RetreatGroupMemberResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RetreatGroupTreeResponse findTree(AdminPrincipal admin) {
        requireRole(admin, AdminRole.STAFF);
        List<RetreatGroupMember> members = retreatGroupMapper.findMembers();
        List<GroupNode> groups = retreatGroupMapper.findGroups()
                .stream()
                .map(group -> new GroupNode(
                        group.id(),
                        group.name(),
                        group.description(),
                        group.displayOrder(),
                        group.active(),
                        members.stream()
                                .filter(member -> member.retreatGroupId().equals(group.id()))
                                .map(member -> new MemberNode(
                                        member.id(),
                                        member.participantId(),
                                        member.participantName(),
                                        member.leader(),
                                        member.displayOrder()
                                ))
                                .toList()
                ))
                .toList();
        return new RetreatGroupTreeResponse(groups);
    }

    @Transactional
    public AdminRegistrationResponse assignParticipant(
            AdminPrincipal admin,
            Long participantId,
            RetreatGroupAssignmentRequest request
    ) {
        requireRole(admin, AdminRole.CHAIR);
        Registration registration = findRegistrationOrThrow(participantId);
        findGroupOrThrow(request.retreatGroupId());
        Long assignedGroupId = retreatGroupMapper.findGroupIdByParticipantId(participantId).orElse(null);
        if (request.retreatGroupId().equals(assignedGroupId)) {
            return AdminRegistrationResponse.detail(registration);
        }

        String previousSnapshot = snapshot(registration);
        if (assignedGroupId == null) {
            retreatGroupMapper.insertMember(request.retreatGroupId(), participantId, false);
        } else {
            retreatGroupMapper.updateMemberGroup(request.retreatGroupId(), participantId);
        }
        return updatedRegistrationResponse(participantId, previousSnapshot, admin.id());
    }

    @Transactional
    public AdminRegistrationResponse removeParticipant(AdminPrincipal admin, Long participantId) {
        requireRole(admin, AdminRole.CHAIR);
        Registration registration = findRegistrationOrThrow(participantId);
        String previousSnapshot = snapshot(registration);
        retreatGroupMapper.deleteMemberByParticipantId(participantId);
        return updatedRegistrationResponse(participantId, previousSnapshot, admin.id());
    }

    @Transactional
    public AdminRegistrationResponse assignLeader(
            AdminPrincipal admin,
            Long groupId,
            RetreatGroupLeaderRequest request
    ) {
        requireRole(admin, AdminRole.CHAIR);
        findGroupOrThrow(groupId);
        Registration registration = findRegistrationOrThrow(request.participantId());
        Long assignedGroupId = retreatGroupMapper.findGroupIdByParticipantId(request.participantId()).orElse(null);
        if (assignedGroupId != null && !assignedGroupId.equals(groupId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RETREAT_GROUP_ASSIGNMENT);
        }

        String previousSnapshot = snapshot(registration);
        retreatGroupMapper.clearGroupLeader(groupId);
        if (assignedGroupId == null) {
            retreatGroupMapper.insertMember(groupId, request.participantId(), true);
        } else {
            retreatGroupMapper.markLeader(groupId, request.participantId());
        }
        retreatGroupMapper.moveMemberToTop(groupId, request.participantId());
        return updatedRegistrationResponse(request.participantId(), previousSnapshot, admin.id());
    }

    @Transactional
    public List<RetreatGroupMemberResponse> updateMemberOrder(
            AdminPrincipal admin,
            Long groupId,
            RetreatGroupMemberOrderRequest request
    ) {
        requireRole(admin, AdminRole.CHAIR);
        findGroupOrThrow(groupId);

        List<Long> requestedIds = request.participantIds();
        Set<Long> requestedIdSet = Set.copyOf(requestedIds);
        Set<Long> currentIdSet = retreatGroupMapper.findMembersByGroupId(groupId)
                .stream()
                .map(RetreatGroupMember::participantId)
                .collect(Collectors.toSet());
        if (requestedIdSet.size() != requestedIds.size() || !requestedIdSet.equals(currentIdSet)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        for (int index = 0; index < requestedIds.size(); index += 1) {
            retreatGroupMapper.updateMemberDisplayOrder(groupId, requestedIds.get(index), index);
        }
        return retreatGroupMapper.findMembersByGroupId(groupId)
                .stream()
                .map(RetreatGroupMemberResponse::from)
                .toList();
    }

    @Transactional
    public List<RetreatGroupMemberResponse> removeLeader(AdminPrincipal admin, Long groupId) {
        requireRole(admin, AdminRole.CHAIR);
        findGroupOrThrow(groupId);
        retreatGroupMapper.clearGroupLeader(groupId);
        return retreatGroupMapper.findMembersByGroupId(groupId)
                .stream()
                .map(RetreatGroupMemberResponse::from)
                .toList();
    }

    private AdminRegistrationResponse updatedRegistrationResponse(
            Long participantId,
            String previousSnapshot,
            Long adminUserId
    ) {
        Registration updated = registrationMapper.findById(participantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        insertAdminHistory(updated.id(), previousSnapshot, snapshot(updated), adminUserId);
        return AdminRegistrationResponse.detail(updated);
    }

    private RetreatGroup findGroupOrThrow(Long id) {
        return retreatGroupMapper.findGroupById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETREAT_GROUP_NOT_FOUND));
    }

    private Registration findRegistrationOrThrow(Long id) {
        return registrationMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGISTRATION_NOT_FOUND));
    }

    private void ensureGroupNameAvailable(Long id, String name) {
        retreatGroupMapper.findGroupByName(name)
                .filter(existing -> id == null || !existing.id().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_RETREAT_GROUP_NAME);
                });
    }

    private void insertAdminHistory(
            Long registrationId,
            String previousSnapshot,
            String newSnapshot,
            Long adminUserId
    ) {
        registrationHistoryMapper.insert(new RegistrationHistoryInsert(
                registrationId,
                RegistrationHistoryChangeType.RETREAT_GROUP_UPDATED,
                previousSnapshot,
                newSnapshot,
                RegistrationActorType.ADMIN,
                adminUserId
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
}
