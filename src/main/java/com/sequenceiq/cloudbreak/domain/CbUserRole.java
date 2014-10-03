package com.sequenceiq.cloudbreak.domain;

public enum CbUserRole {
    DEPLOYER("deployer"),
    ADMIN("admin"),
    USER("user");

    private final String value;

    private CbUserRole(String value) {
        this.value = value;
    }

    public static CbUserRole fromString(String text) {
        if (text != null) {
            for (CbUserRole cbUserRole : CbUserRole.values()) {
                if (text.equalsIgnoreCase(cbUserRole.value)) {
                    return cbUserRole;
                }
            }
        }
        return null;
    }

}
