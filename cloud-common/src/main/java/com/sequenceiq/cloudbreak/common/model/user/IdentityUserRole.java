package com.sequenceiq.cloudbreak.common.model.user;

public enum IdentityUserRole {
    DEPLOYER("deployer"),
    ADMIN("admin"),
    USER("user");

    private final String value;

    IdentityUserRole(String value) {
        this.value = value;
    }

    public static IdentityUserRole fromString(String text) {
        if (text != null) {
            for (IdentityUserRole identityUserRole : IdentityUserRole.values()) {
                if (text.equalsIgnoreCase(identityUserRole.value)) {
                    return identityUserRole;
                }
            }
        }
        return null;
    }

}
