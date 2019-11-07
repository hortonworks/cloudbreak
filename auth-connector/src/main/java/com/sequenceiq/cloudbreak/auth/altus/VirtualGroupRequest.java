package com.sequenceiq.cloudbreak.auth.altus;

public class VirtualGroupRequest {
    private final String accountId;

    private final String environmentCrn;

    private final String adminGroup;

    public VirtualGroupRequest(String environmentCrn, String adminGroup) {
        this.accountId = Crn.fromString(environmentCrn).getAccountId();
        this.environmentCrn = environmentCrn;
        this.adminGroup = adminGroup;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public String getAdminGroup() {
        return adminGroup;
    }
}
