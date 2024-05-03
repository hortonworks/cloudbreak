package com.sequenceiq.datalake.flow.detach.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeResizeFlowChainStartEvent extends SdxEvent {

    public static final String SDX_RESIZE_FLOW_CHAIN_START_EVENT = "DatalakeResizeFlowChainStartEvent";

    private final SdxCluster sdxCluster;

    private final String backupLocation;

    private final boolean backup;

    private final boolean restore;

    private final DatalakeDrSkipOptions skipOptions;

    private final boolean validationOnly;

    @SuppressWarnings("ExecutableStatementCount")
    @JsonCreator
    public DatalakeResizeFlowChainStartEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("sdxCluster") SdxCluster sdxCluster,
            @JsonProperty("userId") String userId,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("backup") boolean backup,
            @JsonProperty("restore") boolean restore,
            @JsonProperty("skipOptions") DatalakeDrSkipOptions skipOptions,
            @JsonProperty("validationOnly") boolean validationOnly) {
        super(sdxId, userId);
        this.sdxCluster = sdxCluster;
        this.backupLocation = backupLocation;
        this.backup = backup;
        this.restore = restore;
        this.skipOptions = skipOptions;
        this.validationOnly = validationOnly;
    }

    public boolean shouldTakeBackup() {
        return backup;
    }

    public boolean shouldPerformRestore() {
        return restore;
    }

    public boolean isBackup() {
        return backup;
    }

    public boolean isRestore() {
        return restore;
    }

    public SdxCluster getSdxCluster() {
        return sdxCluster;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public DatalakeDrSkipOptions getSkipOptions() {
        return skipOptions;
    }

    public boolean isValidationOnly() {
        return validationOnly;
    }

    @Override
    public String selector() {
        return SDX_RESIZE_FLOW_CHAIN_START_EVENT;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(DatalakeResizeFlowChainStartEvent.class, other,
                event -> Objects.equals(sdxCluster.getClusterShape(), event.sdxCluster.getClusterShape())
                        && Objects.equals(sdxCluster.getCrn(), event.sdxCluster.getCrn())
                        && Objects.equals(backupLocation, event.backupLocation)
                        && backup == event.backup
                        && restore == event.restore);
    }
}
