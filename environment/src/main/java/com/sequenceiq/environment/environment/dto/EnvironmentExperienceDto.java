package com.sequenceiq.environment.environment.dto;

public class EnvironmentExperienceDto {

    private final String name;

    private final String crn;

    private final String accountId;

    private final String cloudPlatform;

    private EnvironmentExperienceDto(String name, String crn, String accountId, String cloudPlatform) {
        this.crn = crn;
        this.name = name;
        this.accountId = accountId;
        this.cloudPlatform = cloudPlatform;
    }

    public String getName() {
        return name;
    }

    public String getCrn() {
        return crn;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    @Override
    public String toString() {
        return "EnvironmentExperienceDto{" +
                "name='" + name + '\'' +
                ", crn='" + crn + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", accountId='" + accountId + '\'' +
                '}';
    }

    public static final class Builder {

        private String name;

        private String crn;

        private String accountId;

        private String cloudPlatform;

        public Builder() {
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withCrn(String crn) {
            this.crn = crn;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public EnvironmentExperienceDto build() {
            return new EnvironmentExperienceDto(name, crn, accountId, cloudPlatform);
        }

    }

}
