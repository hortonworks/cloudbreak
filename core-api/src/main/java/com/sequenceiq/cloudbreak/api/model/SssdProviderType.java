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
}
