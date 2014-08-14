package com.sequenceiq.cloudbreak.domain;

public enum UserRole {
    DEPLOYER("DEPLOYER"),
    ACCOUNT_USER("ACCOUNT_USER"),
    ACCOUNT_ADMIN("ACCOUNT_ADMIN");

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
