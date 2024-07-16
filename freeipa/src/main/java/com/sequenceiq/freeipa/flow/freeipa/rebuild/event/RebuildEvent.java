package com.sequenceiq.freeipa.flow.freeipa.rebuild.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RebuildEvent extends StackEvent {

    private final String instanceToRestoreFqdn;

    private final String fullBackupStorageLocation;

    private final String dataBackupStorageLocation;

    private final String operationId;

    @JsonCreator
    public RebuildEvent(@JsonProperty("resourceId") Long stackId,
            @JsonProperty("instanceToRestoreFqdn") String instanceToRestoreFqdn,
            @JsonProperty("fullBackupStorageLocation") String fullBackupStorageLocation,
            @JsonProperty("dataBackupStorageLocation") String dataBackupStorageLocation,
            @JsonProperty("operationId") String operationId) {
        super(stackId);
        this.instanceToRestoreFqdn = instanceToRestoreFqdn;
        this.fullBackupStorageLocation = fullBackupStorageLocation;
        this.dataBackupStorageLocation = dataBackupStorageLocation;
        this.operationId = operationId;
    }

    public String getInstanceToRestoreFqdn() {
        return instanceToRestoreFqdn;
    }

    public String getFullBackupStorageLocation() {
        return fullBackupStorageLocation;
    }

    public String getDataBackupStorageLocation() {
        return dataBackupStorageLocation;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public String toString() {
        return "RebuildEvent{" +
                "instanceToRestoreFqdn='" + instanceToRestoreFqdn + '\'' +
                ", fullBackupStorageLocation='" + fullBackupStorageLocation + '\'' +
                ", dataBackupStorageLocation='" + dataBackupStorageLocation + '\'' +
                ", operationId='" + operationId + '\'' +
                "} " + super.toString();
    }
}
