package com.sequenceiq.environment.environment.dto;

public class EnvironmentChangeCredentialDto {

    private String credentialName;

    public EnvironmentChangeCredentialDto(Builder builder) {
        this.credentialName = builder.credentialName;
    }

    public String getCredentialName() {
        return credentialName;
    }

    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    @Override
    public String toString() {
        return "EnvironmentChangeCredentialDto{" +
                "credentialName='" + credentialName + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String credentialName;

        private Builder() {
        }

        public Builder withCredentialName(String credentialName) {
            this.credentialName = credentialName;
            return this;
        }

        public EnvironmentChangeCredentialDto build() {
            return new EnvironmentChangeCredentialDto(this);
        }

    }

}
