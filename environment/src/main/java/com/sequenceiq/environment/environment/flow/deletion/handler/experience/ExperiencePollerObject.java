package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

public class ExperiencePollerObject {

    private final String environmentCrn;

    private final String environmentName;

    private final String accountId;

    public ExperiencePollerObject(String environmentCrn, String environmentName, String accountId) {
        this.environmentCrn = environmentCrn;
        this.environmentName = environmentName;
        this.accountId = accountId;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public String getAccountId() {
        return accountId;
    }

}
