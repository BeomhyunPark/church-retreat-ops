package com.gmc.retreat.admin.domain;

public enum AdminRole {
    STAFF(1),
    CHAIR(2),
    PASTOR(3),
    SYSTEM_ADMIN(4);

    private final int level;

    AdminRole(int level) {
        this.level = level;
    }

    public boolean hasAuthorityAtLeast(AdminRole requiredRole) {
        return this.level >= requiredRole.level;
    }

    public String authority() {
        return "ROLE_" + name();
    }
}
