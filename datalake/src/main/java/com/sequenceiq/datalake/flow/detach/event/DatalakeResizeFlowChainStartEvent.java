package com.sequenceiq.datalake.flow.detach.event;

import java.util.Objects;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeResizeFlowChainStartEvent extends SdxEvent {

    public static final String SDX_RESIZE_FLOW_CHAIN_START_EVENT = "DatalakeResizeFlowChainStartEvent";

    private SdxCluster sdxCluster;

    private final String backupLocation;

    private final boolean backup;

    public DatalakeResizeFlowChainStartEvent(Long sdxId, SdxCluster newSdxCluster, String userId, String backupLocation, boolean backup) {
        super(sdxId, userId);
        this.sdxCluster = newSdxCluster;
        this.backupLocation = backupLocation;
        this.backup = backup;
    }

    public boolean shouldTakeBackup() {
        return backup;
    }

    public SdxCluster getSdxCluster() {
        return sdxCluster;
    }

    public String getBackupLocation() {
        return backupLocation;
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
                        && backup == event.backup);
    }
}
