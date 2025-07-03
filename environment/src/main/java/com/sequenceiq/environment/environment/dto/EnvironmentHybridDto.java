package com.sequenceiq.environment.environment.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = EnvironmentHybridDto.Builder.class)
public class EnvironmentHybridDto {

    private final String remoteEnvironmentCrn;

    private EnvironmentHybridDto(Builder builder) {
        remoteEnvironmentCrn = builder.remoteEnvironmentCrn;
    }

    public String getRemoteEnvironmentCrn() {
        return remoteEnvironmentCrn;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "EnvironmentHybridDto{" +
                "remoteEnvironmentCrn='" + remoteEnvironmentCrn +
                '}';
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private String remoteEnvironmentCrn;

        private Builder() {
        }

        public Builder withRemoteEnvironmentCrn(String remoteEnvironmentCrn) {
            this.remoteEnvironmentCrn = remoteEnvironmentCrn;
            return this;
        }

        public EnvironmentHybridDto build() {
            return new EnvironmentHybridDto(this);
        }
    }
}
