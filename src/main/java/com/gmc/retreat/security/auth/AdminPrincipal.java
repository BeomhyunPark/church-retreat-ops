package com.gmc.retreat.security.auth;

import com.gmc.retreat.admin.domain.AdminRole;
import com.gmc.retreat.admin.domain.AdminStatus;
import com.gmc.retreat.admin.domain.AdminUser;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AdminPrincipal(
        Long id,
        String email,
        String name,
        AdminRole role,
        AdminStatus status
) {
    public static AdminPrincipal from(AdminUser adminUser) {
        return new AdminPrincipal(
                adminUser.id(),
                adminUser.email(),
                adminUser.name(),
                adminUser.role(),
                adminUser.status()
        );
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }
}
