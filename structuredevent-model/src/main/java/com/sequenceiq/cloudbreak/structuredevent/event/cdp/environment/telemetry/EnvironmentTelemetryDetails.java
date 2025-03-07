package com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.telemetry;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = EnvironmentTelemetryDetails.Builder.class)
public record EnvironmentTelemetryDetails(
        String storageLocationBase,
        String backupStorageLocationBase
) {

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private String storageLocationBase;

        private String backupStorageLocationBase;

        public Builder withStorageLocationBase(String storageLocationBase) {
            this.storageLocationBase = storageLocationBase;
            return this;
        }

        public Builder withBackupStorageLocationBase(String backupStorageLocationBase) {
            this.backupStorageLocationBase = backupStorageLocationBase;
            return this;
        }

        public EnvironmentTelemetryDetails build() {
            return new EnvironmentTelemetryDetails(storageLocationBase, backupStorageLocationBase);
        }
    }
}
