package com.gmc.retreat.admin.service;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.admin.domain.AdminStatus;
import com.gmc.retreat.admin.mapper.AdminUserInsert;
import com.gmc.retreat.admin.mapper.AdminUserMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SystemAdminBootstrapper implements ApplicationRunner {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final SystemAdminBootstrapProperties properties;

    public SystemAdminBootstrapper(
            AdminUserMapper adminUserMapper,
            PasswordEncoder passwordEncoder,
            SystemAdminBootstrapProperties properties
    ) {
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminUserMapper.findByEmail(properties.email()).isPresent()) {
            return;
        }

        AdminUserInsert adminUser = new AdminUserInsert(
                properties.email(),
                passwordEncoder.encode(properties.password()),
                properties.name(),
                AdminRole.SYSTEM_ADMIN,
                AdminStatus.ACTIVE
        );
        adminUserMapper.insert(adminUser);
    }
}
