package com.sequenceiq.datalake.entity;

public class Credential {

    private final String cloudPlatform;

    private final String name;

    private final String attributes;

    private final String crn;

    private final String accountId;

    public Credential(String cloudPlatform, String name, String attributes, String crn, String accountId) {
        this.cloudPlatform = cloudPlatform;
        this.name = name;
        this.attributes = attributes;
        this.accountId = accountId;
        this.crn = crn;
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
}
