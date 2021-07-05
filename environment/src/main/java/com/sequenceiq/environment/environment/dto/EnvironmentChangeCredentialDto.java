package com.sequenceiq.environment.environment.dto;

public class EnvironmentChangeCredentialDto {

    private String credentialName;

    public EnvironmentChangeCredentialDto(String credentialName) {
        this.credentialName = credentialName;
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

    public static final class EnvironmentChangeCredentialDtoBuilder {

        private String credentialName;

        private EnvironmentChangeCredentialDtoBuilder() {
        }

        public static EnvironmentChangeCredentialDtoBuilder anEnvironmentChangeCredentialDto() {
            return new EnvironmentChangeCredentialDtoBuilder();
        }

        public EnvironmentChangeCredentialDtoBuilder withCredentialName(String credentialName) {
            this.credentialName = credentialName;
            return this;
        }

        public EnvironmentChangeCredentialDto build() {
            return new EnvironmentChangeCredentialDto(credentialName);
        }

    }

}
