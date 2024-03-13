package com.sequenceiq.freeipa.flow.freeipa.rebuild.event.backup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ValidateBackupRequest extends StackEvent {

    private final String fullBackupStorageLocation;

    private final String dataBackupStorageLocation;

    @JsonCreator
    public ValidateBackupRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("fullBackupStorageLocation") String fullBackupStorageLocation,
            @JsonProperty("dataBackupStorageLocation") String dataBackupStorageLocation) {
        super(stackId);
        this.fullBackupStorageLocation = fullBackupStorageLocation;
        this.dataBackupStorageLocation = dataBackupStorageLocation;
    }

    public String getFullBackupStorageLocation() {
        return fullBackupStorageLocation;
    }

    public String getDataBackupStorageLocation() {
        return dataBackupStorageLocation;
    }

    @Override
    public String toString() {
        return "ValidateBackupRequest{" +
                ", fullBackupStorageLocation='" + fullBackupStorageLocation + '\'' +
                ", dataBackupStorageLocation='" + dataBackupStorageLocation + '\'' +
                "} " + super.toString();
    }
}
