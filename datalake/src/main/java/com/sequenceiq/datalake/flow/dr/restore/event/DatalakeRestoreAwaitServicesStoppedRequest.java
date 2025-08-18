package com.sequenceiq.datalake.flow.dr.restore.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeRestoreAwaitServicesStoppedRequest extends SdxEvent {

    private final DatalakeRestoreParams datalakeRestoreParams;

    @JsonCreator
    public DatalakeRestoreAwaitServicesStoppedRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("datalakeRestoreParams") DatalakeRestoreParams datalakeRestoreParams) {
        super(sdxId, userId);
        this.datalakeRestoreParams = datalakeRestoreParams;
    }

    public static DatalakeRestoreAwaitServicesStoppedRequest from(DatalakeDatabaseRestoreStartEvent startEvent) {
        DatalakeRestoreParams params = new DatalakeRestoreParams(
                startEvent.getRestoreId(),
                startEvent.getDrStatus(),
                startEvent.getBackupLocation(),
                startEvent.getBackupId(),
                startEvent.getDatabaseMaxDurationInMin(),
                startEvent.isValidationOnly());
        return new DatalakeRestoreAwaitServicesStoppedRequest(
                startEvent.getResourceId(),
                startEvent.getUserId(),
                params
        );
    }

    public SdxOperation getDrStatus() {
        return datalakeRestoreParams.getDrStatus();
    }

    public String getOperationId() {
        return datalakeRestoreParams.getOperationId();
    }

    public String getBackupLocation() {
        return datalakeRestoreParams.getBackupLocation();
    }

    public String getBackupId() {
        return datalakeRestoreParams.getBackupId();
    }

    public boolean isValidationOnly() {
        return datalakeRestoreParams.isValidationOnly();
    }

    public int getDatabaseMaxDurationInMin() {
        return datalakeRestoreParams.getDatabaseMaxDurationInMin();
    }

    public DatalakeRestoreParams getDatalakeRestoreParams() {
        return datalakeRestoreParams;
    }
}
