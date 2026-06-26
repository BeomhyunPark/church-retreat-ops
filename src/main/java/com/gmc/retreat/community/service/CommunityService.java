package com.gmc.retreat.community.service;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.community.domain.ChurchCell;
import com.gmc.retreat.community.domain.ChurchMiddleGroup;
import com.gmc.retreat.community.dto.ActiveUpdateRequest;
import com.gmc.retreat.community.dto.ChurchCellRequest;
import com.gmc.retreat.community.dto.ChurchCellResponse;
import com.gmc.retreat.community.dto.ChurchMiddleGroupRequest;
import com.gmc.retreat.community.dto.ChurchMiddleGroupResponse;
import com.gmc.retreat.community.dto.CommunityTreeResponse;
import com.gmc.retreat.community.dto.CommunityTreeResponse.CellNode;
import com.gmc.retreat.community.dto.CommunityTreeResponse.MiddleGroupNode;
import com.gmc.retreat.community.mapper.ChurchCellUpsert;
import com.gmc.retreat.community.mapper.ChurchMiddleGroupUpsert;
import com.gmc.retreat.community.mapper.CommunityMapper;
import com.gmc.retreat.error.BusinessException;
import com.gmc.retreat.error.ErrorCode;
import com.gmc.retreat.security.auth.AdminPrincipal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CommunityService {

    private final CommunityMapper communityMapper;

    public CommunityService(CommunityMapper communityMapper) {
        this.communityMapper = communityMapper;
    }

    @Transactional(readOnly = true)
    public List<ChurchMiddleGroupResponse> findMiddleGroups(AdminPrincipal admin) {
        requireRole(admin, AdminRole.STAFF);
        return communityMapper.findMiddleGroups()
                .stream()
                .map(ChurchMiddleGroupResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChurchMiddleGroupResponse findMiddleGroup(AdminPrincipal admin, Long id) {
        requireRole(admin, AdminRole.STAFF);
        return ChurchMiddleGroupResponse.from(findMiddleGroupOrThrow(id));
    }

    @Transactional
    public ChurchMiddleGroupResponse createMiddleGroup(AdminPrincipal admin, ChurchMiddleGroupRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        String name = normalizeRequired(request.name());
        ensureMiddleGroupNameAvailable(null, name);
        ChurchMiddleGroupUpsert insert = new ChurchMiddleGroupUpsert(
                null,
                name,
                normalizeOptional(request.elderName()),
                normalizeOptional(request.description()),
                request.displayOrder()
        );
        communityMapper.insertMiddleGroup(insert);
        return ChurchMiddleGroupResponse.from(findMiddleGroupOrThrow(insert.getId()));
    }

    @Transactional
    public ChurchMiddleGroupResponse updateMiddleGroup(
            AdminPrincipal admin,
            Long id,
            ChurchMiddleGroupRequest request
    ) {
        requireRole(admin, AdminRole.CHAIR);
        findMiddleGroupOrThrow(id);
        String name = normalizeRequired(request.name());
        ensureMiddleGroupNameAvailable(id, name);
        communityMapper.updateMiddleGroup(new ChurchMiddleGroupUpsert(
                id,
                name,
                normalizeOptional(request.elderName()),
                normalizeOptional(request.description()),
                request.displayOrder()
        ));
        return ChurchMiddleGroupResponse.from(findMiddleGroupOrThrow(id));
    }

    @Transactional
    public ChurchMiddleGroupResponse updateMiddleGroupActive(
            AdminPrincipal admin,
            Long id,
            ActiveUpdateRequest request
    ) {
        requireRole(admin, AdminRole.CHAIR);
        findMiddleGroupOrThrow(id);
        communityMapper.updateMiddleGroupActive(id, request.active());
        return ChurchMiddleGroupResponse.from(findMiddleGroupOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ChurchCellResponse> findCells(AdminPrincipal admin, Long middleGroupId, Boolean active) {
        requireRole(admin, AdminRole.STAFF);
        if (middleGroupId != null) {
            findMiddleGroupOrThrow(middleGroupId);
        }
        return communityMapper.findCells(middleGroupId, active)
                .stream()
                .map(ChurchCellResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChurchCellResponse findCell(AdminPrincipal admin, Long id) {
        requireRole(admin, AdminRole.STAFF);
        return ChurchCellResponse.from(findCellOrThrow(id));
    }

    @Transactional
    public ChurchCellResponse createCell(AdminPrincipal admin, ChurchCellRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        findMiddleGroupOrThrow(request.middleGroupId());
        String name = normalizeRequired(request.name());
        ensureCellNameAvailable(null, request.middleGroupId(), name);
        ChurchCellUpsert insert = new ChurchCellUpsert(
                null,
                request.middleGroupId(),
                name,
                normalizeOptional(request.cellLeaderName()),
                normalizeOptional(request.description()),
                request.displayOrder()
        );
        communityMapper.insertCell(insert);
        return ChurchCellResponse.from(findCellOrThrow(insert.getId()));
    }

    @Transactional
    public ChurchCellResponse updateCell(AdminPrincipal admin, Long id, ChurchCellRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        findCellOrThrow(id);
        findMiddleGroupOrThrow(request.middleGroupId());
        String name = normalizeRequired(request.name());
        ensureCellNameAvailable(id, request.middleGroupId(), name);
        communityMapper.updateCell(new ChurchCellUpsert(
                id,
                request.middleGroupId(),
                name,
                normalizeOptional(request.cellLeaderName()),
                normalizeOptional(request.description()),
                request.displayOrder()
        ));
        return ChurchCellResponse.from(findCellOrThrow(id));
    }

    @Transactional
    public ChurchCellResponse updateCellActive(AdminPrincipal admin, Long id, ActiveUpdateRequest request) {
        requireRole(admin, AdminRole.CHAIR);
        findCellOrThrow(id);
        communityMapper.updateCellActive(id, request.active());
        return ChurchCellResponse.from(findCellOrThrow(id));
    }

    @Transactional(readOnly = true)
    public CommunityTreeResponse findTree(AdminPrincipal admin) {
        requireRole(admin, AdminRole.STAFF);
        List<ChurchCell> cells = communityMapper.findCells(null, null);
        List<MiddleGroupNode> middleGroups = communityMapper.findMiddleGroups()
                .stream()
                .map(middleGroup -> new MiddleGroupNode(
                        middleGroup.id(),
                        middleGroup.name(),
                        middleGroup.elderName(),
                        middleGroup.description(),
                        middleGroup.displayOrder(),
                        middleGroup.active(),
                        cells.stream()
                                .filter(cell -> cell.middleGroupId().equals(middleGroup.id()))
                                .map(cell -> new CellNode(
                                        cell.id(),
                                        cell.name(),
                                        cell.cellLeaderName(),
                                        cell.description(),
                                        cell.displayOrder(),
                                        cell.active()
                                ))
                                .toList()
                ))
                .toList();
        return new CommunityTreeResponse(middleGroups);
    }

    public void ensureCellExists(Long id) {
        findCellOrThrow(id);
    }

    private ChurchMiddleGroup findMiddleGroupOrThrow(Long id) {
        return communityMapper.findMiddleGroupById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_NOT_FOUND));
    }

    private ChurchCell findCellOrThrow(Long id) {
        return communityMapper.findCellById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_NOT_FOUND));
    }

    private void ensureMiddleGroupNameAvailable(Long id, String name) {
        communityMapper.findMiddleGroupByName(name)
                .filter(existing -> id == null || !existing.id().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_COMMUNITY_NAME);
                });
    }

    private void ensureCellNameAvailable(Long id, Long middleGroupId, String name) {
        communityMapper.findCellByMiddleGroupAndName(middleGroupId, name)
                .filter(existing -> id == null || !existing.id().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_COMMUNITY_NAME);
                });
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
