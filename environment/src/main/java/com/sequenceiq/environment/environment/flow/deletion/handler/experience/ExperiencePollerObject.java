package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import java.util.StringJoiner;

public class ExperiencePollerObject {

    private final String environmentCrn;

    private final String environmentName;

    private final String cloudPlatform;

    private final String accountId;

    public ExperiencePollerObject(String environmentCrn, String environmentName, String cloudPlatform, String accountId) {
        this.environmentName = environmentName;
        this.environmentCrn = environmentCrn;
        this.cloudPlatform = cloudPlatform;
        this.accountId = accountId;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public String getAccountId() {
        return accountId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ExperiencePollerObject.class.getSimpleName() + "[", "]")
                .add("environmentCrn='" + environmentCrn + "'")
                .add("environmentName='" + environmentName + "'")
                .add("cloudPlatform='" + cloudPlatform + "'")
                .add("accountId='" + accountId + "'")
                .toString();
    }
}
