package com.sequenceiq.freeipa.dto;

public class Credential {

    private final String cloudPlatform;

    private final String name;

    private final String attributes;

    private final String crn;

    private final String accountId;

    private final boolean govCloud;

    public Credential(String cloudPlatform, String name, String attributes, String crn, String accountId) {
        this(cloudPlatform, name, attributes, crn, accountId, false);
    }

    public Credential(String cloudPlatform, String name, String attributes, String crn, String accountId, boolean govCloud) {
        this.cloudPlatform = cloudPlatform;
        this.name = name;
        this.attributes = attributes;
        this.crn = crn;
        this.accountId = accountId;
        this.govCloud = govCloud;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public String getName() {
        return name;
    }

    public String getAttributes() {
        return attributes;
    }

    public String getCrn() {
        return crn;
    }

    public String getAccountId() {
        return accountId;
    }

    public boolean isGovCloud() {
        return govCloud;
    }
}
