package com.sequenceiq.environment.environment.dto.dataservices;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;


@JsonDeserialize(builder = AzureDataServiceParameters.Builder.class)
public record AzureDataServiceParameters(@NotNull String sharedManagedIdentity) implements Serializable {

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private @NotNull String sharedManagedIdentity;

        private Builder() {
        }

        public Builder withSharedManagedIdentity(String sharedManagedIdentity) {
            this.sharedManagedIdentity = sharedManagedIdentity;
            return this;
        }

        public AzureDataServiceParameters build() {
            return new AzureDataServiceParameters(sharedManagedIdentity);
        }
    }
}
