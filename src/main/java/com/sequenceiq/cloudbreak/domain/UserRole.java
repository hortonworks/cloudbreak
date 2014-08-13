package com.sequenceiq.cloudbreak.domain;

public enum UserRole {
    DEPLOYER("DEPLOYER"),
    COMPANY_USER("COMPANY_USER"),
    COMPANY_ADMIN("COMPANY_ADMIN"),
    REGULAR_USER("REGULAR_USER");

    private final String value;

    private UserRole(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

    public String role() {
        return String.format("ROLE_%s", this.value);
    }

}
