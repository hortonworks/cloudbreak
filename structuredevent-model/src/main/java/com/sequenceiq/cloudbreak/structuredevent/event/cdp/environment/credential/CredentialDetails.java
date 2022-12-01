package com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = CredentialDetails.Builder.class)
public class CredentialDetails {
    private final CredentialType credentialType;

    private CredentialDetails(CredentialType credentialType) {
        this.credentialType = credentialType;
    }

    public CredentialType getCredentialType() {
        return credentialType;
    }

    @Override
    public String toString() {
        return "CredentialDetails{" +
                "credentialType=" + credentialType +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private CredentialType credentialType;

        private Builder() {
        }

        public Builder withCredentialType(CredentialType credentialType) {
            this.credentialType = credentialType;
            return this;
        }

        public CredentialDetails build() {
            return new CredentialDetails(credentialType);
        }
    }
}
