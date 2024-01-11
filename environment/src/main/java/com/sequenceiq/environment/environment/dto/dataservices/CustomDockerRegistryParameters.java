package com.sequenceiq.environment.environment.dto.dataservices;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;


@JsonDeserialize(builder = CustomDockerRegistryParameters.Builder.class)
public record CustomDockerRegistryParameters(@NotNull String crn) implements Serializable {

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private @NotNull String crn;

        private Builder() {
        }

        public Builder withCrn(String crn) {
            this.crn = crn;
            return this;
        }

        public CustomDockerRegistryParameters build() {
            return new CustomDockerRegistryParameters(crn);
        }
    }
}
