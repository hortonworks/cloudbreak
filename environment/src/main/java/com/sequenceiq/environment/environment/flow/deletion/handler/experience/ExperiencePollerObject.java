package com.sequenceiq.environment.environment.flow.deletion.handler.experience;

import java.util.StringJoiner;

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

    @Override
    public String toString() {
        return new StringJoiner(", ", ExperiencePollerObject.class.getSimpleName() + "[", "]")
                .add("environmentCrn='" + environmentCrn + "'")
                .add("environmentName='" + environmentName + "'")
                .add("accountId='" + accountId + "'")
                .toString();
    }
}
