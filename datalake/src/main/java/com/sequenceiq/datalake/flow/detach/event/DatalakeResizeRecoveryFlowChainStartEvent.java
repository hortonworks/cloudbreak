package com.sequenceiq.datalake.flow.detach.event;

import java.util.Objects;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeResizeRecoveryFlowChainStartEvent extends SdxEvent {
    public static final String SDX_RESIZE_RECOVERY_FLOW_CHAIN_START_EVENT = "DatalakeResizeRecoveryFlowChainStartEvent";

    private final SdxCluster oldCluster;

    private final SdxCluster newCluster;

    public DatalakeResizeRecoveryFlowChainStartEvent(SdxCluster oldCluster, SdxCluster newCluster, String userId) {
        super(oldCluster.getId(), userId);
        this.oldCluster = oldCluster;
        this.newCluster = newCluster;
    }

    public SdxCluster getOldCluster() {
        return oldCluster;
    }

    public SdxCluster getNewCluster() {
        return newCluster;
    }

    @Override
    public String selector() {
        return SDX_RESIZE_RECOVERY_FLOW_CHAIN_START_EVENT;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(DatalakeResizeRecoveryFlowChainStartEvent.class, other, event ->
            Objects.equals(oldCluster, event.oldCluster) && Objects.equals(newCluster, event.newCluster)
        );
    }

    @Override
    public String toString() {
        return selector() + '{' +
                "oldCluster: '" + oldCluster.toString() + "'," +
                "newCluster: '" + (newCluster == null ? "null" : newCluster.toString()) + "'}";
    }
}
