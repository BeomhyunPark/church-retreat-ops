package com.gmc.retreat.admin.service;

import com.gmc.retreat.admin.domain.AdminStatus;
import com.gmc.retreat.admin.domain.AdminUser;
import com.gmc.retreat.admin.dto.AdminLoginResponse;
import com.gmc.retreat.admin.dto.AdminProfileResponse;
import com.gmc.retreat.admin.dto.AdminSummaryResponse;
import com.gmc.retreat.admin.mapper.AdminUserMapper;
import com.gmc.retreat.error.BusinessException;
import com.gmc.retreat.error.ErrorCode;
import com.gmc.retreat.security.auth.AdminPrincipal;
import com.gmc.retreat.security.jwt.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AdminAuthService(
            AdminUserMapper adminUserMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AdminLoginResponse login(String email, String password) {
        AdminUser adminUser = adminUserMapper.findByEmail(email)
                .filter(user -> passwordEncoder.matches(password, user.passwordHash()))
                .filter(user -> user.status() == AdminStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_ADMIN_CREDENTIALS));

        adminUserMapper.updateLastLoginAt(adminUser.id());

        String accessToken = jwtTokenProvider.createAccessToken(AdminPrincipal.from(adminUser));
        return new AdminLoginResponse(accessToken, "Bearer", AdminSummaryResponse.from(adminUser));
    }

    @Transactional(readOnly = true)
    public AdminProfileResponse getProfile(Long adminUserId) {
        AdminUser adminUser = adminUserMapper.findById(adminUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (adminUser.status() != AdminStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return AdminProfileResponse.from(adminUser);
    }
}
