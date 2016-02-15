package com.sequenceiq.cloudbreak.api.model;

public enum SssdProviderType {

    LDAP("ldap"),
    ACTIVE_DIRECTORY("ad"),
    IPA("ipa");

    private String type;

    SssdProviderType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static SssdProviderType fromString(String providerType) {
        for (SssdProviderType type : SssdProviderType.values()) {
            if (type.type.equalsIgnoreCase(providerType)) {
                return type;
            }
        }
        return null;
    }
}
