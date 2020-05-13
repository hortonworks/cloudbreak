package com.sequenceiq.environment.environment.dto;

public class EnvironmentExperienceDto {

    private final String name;

    private final String crn;

    private final String accountId;

    private EnvironmentExperienceDto(String name, String crn, String accountId) {
        this.name = name;
        this.crn = crn;
        this.accountId = accountId;
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

    @Override
    public String toString() {
        return "EnvironmentExperienceDto{" +
                "name='" + name + '\'' +
                ", crn='" + crn + '\'' +
                ", accountId='" + accountId + '\'' +
                '}';
    }

    public static final class Builder {

        private String name;

        private String crn;

        private String accountId;

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

        public EnvironmentExperienceDto build() {
            return new EnvironmentExperienceDto(name, crn, accountId);
        }

    }

}
