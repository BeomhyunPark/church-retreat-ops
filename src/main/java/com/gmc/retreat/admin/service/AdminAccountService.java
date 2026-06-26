package com.gmc.retreat.admin.service;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.admin.domain.AdminStatus;
import com.gmc.retreat.admin.domain.AdminUser;
import com.gmc.retreat.admin.dto.AdminAccountResponse;
import com.gmc.retreat.admin.dto.AdminCreateRequest;
import com.gmc.retreat.admin.dto.AdminPasswordResetRequest;
import com.gmc.retreat.admin.dto.AdminStatusUpdateRequest;
import com.gmc.retreat.admin.dto.AdminUpdateRequest;
import com.gmc.retreat.admin.mapper.AdminUserInsert;
import com.gmc.retreat.admin.mapper.AdminUserMapper;
import com.gmc.retreat.error.BusinessException;
import com.gmc.retreat.error.ErrorCode;
import com.gmc.retreat.security.auth.AdminPrincipal;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminAccountService {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountService(AdminUserMapper adminUserMapper, PasswordEncoder passwordEncoder) {
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AdminAccountResponse> findAll(AdminPrincipal admin) {
        requireRole(admin, AdminRole.SYSTEM_ADMIN);
        return adminUserMapper.findAll()
                .stream()
                .map(AdminAccountResponse::from)
                .toList();
    }

    @Transactional
    public AdminAccountResponse create(AdminPrincipal admin, AdminCreateRequest request) {
        requireRole(admin, AdminRole.SYSTEM_ADMIN);
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email);
        adminUserMapper.insert(new AdminUserInsert(
                email,
                passwordEncoder.encode(request.password()),
                request.name().trim(),
                request.role(),
                AdminStatus.ACTIVE
        ));
        return AdminAccountResponse.from(findByEmailOrThrow(email));
    }

    @Transactional
    public AdminAccountResponse update(AdminPrincipal admin, Long id, AdminUpdateRequest request) {
        requireRole(admin, AdminRole.SYSTEM_ADMIN);
        findByIdOrThrow(id);
        adminUserMapper.update(id, request.name().trim(), request.role());
        return AdminAccountResponse.from(findByIdOrThrow(id));
    }

    @Transactional
    public AdminAccountResponse updateStatus(AdminPrincipal admin, Long id, AdminStatusUpdateRequest request) {
        requireRole(admin, AdminRole.SYSTEM_ADMIN);
        findByIdOrThrow(id);
        if (admin.id().equals(id)) {
            throw new BusinessException(ErrorCode.ADMIN_SELF_STATUS_CHANGE_FORBIDDEN);
        }
        adminUserMapper.updateStatus(id, request.status());
        return AdminAccountResponse.from(findByIdOrThrow(id));
    }

    @Transactional
    public void resetPassword(AdminPrincipal admin, Long id, AdminPasswordResetRequest request) {
        requireRole(admin, AdminRole.SYSTEM_ADMIN);
        findByIdOrThrow(id);
        adminUserMapper.updatePasswordHash(id, passwordEncoder.encode(request.newPassword()));
    }

    private AdminUser findByIdOrThrow(Long id) {
        return adminUserMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND));
    }

    private AdminUser findByEmailOrThrow(String email) {
        return adminUserMapper.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND));
    }

    private void ensureEmailAvailable(String email) {
        adminUserMapper.findByEmail(email).ifPresent(existing -> {
            throw new BusinessException(ErrorCode.DUPLICATE_ADMIN_EMAIL);
        });
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return email.trim().toLowerCase();
    }

    private void requireRole(AdminPrincipal admin, AdminRole requiredRole) {
        if (admin == null || !admin.role().hasAuthorityAtLeast(requiredRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
