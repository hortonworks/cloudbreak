package com.sequenceiq.environment.environment.dto;

import com.sequenceiq.environment.environment.domain.Environment;

public class EnvironmentExperienceDto {

    private final String name;

    private final String crn;

    private final String accountId;

    private final String cloudPlatform;

    private EnvironmentExperienceDto(Builder builder) {
        this.crn = builder.crn;
        this.name = builder.name;
        this.accountId = builder.accountId;
        this.cloudPlatform = builder.cloudPlatform;
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

    public static EnvironmentExperienceDto fromEnvironment(Environment environment) {
        return new Builder().fromEnvironment(environment);
    }

    public static EnvironmentExperienceDto fromEnvironmentDto(EnvironmentDto environmentDto) {
        return new Builder().fromEnvironmentDto(environmentDto);
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String name;

        private String crn;

        private String accountId;

        private String cloudPlatform;

        private Builder() {
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
            return new EnvironmentExperienceDto(this);
        }

        public EnvironmentExperienceDto fromEnvironment(Environment environment) {
            return withName(environment.getName())
                    .withName(environment.getName())
                    .withCrn(environment.getResourceCrn())
                    .withAccountId(environment.getAccountId())
                    .withCloudPlatform(environment.getCloudPlatform())
                    .build();
        }

        public EnvironmentExperienceDto fromEnvironmentDto(EnvironmentDto environmentDto) {
            return withName(environmentDto.getName())
                    .withCrn(environmentDto.getResourceCrn())
                    .withAccountId(accountId)
                    .withCloudPlatform(environmentDto.getCloudPlatform())
                    .build();
        }

    }

}
