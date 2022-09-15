package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

import java.time.LocalDateTime;

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.core.FlowParameters;

public class UpgradeCcmContext extends ClusterViewContext {

    private final Tunnel oldTunnel;

    private final LocalDateTime revertTime;

    public UpgradeCcmContext(FlowParameters flowParameters, StackView stack, ClusterView cluster, Tunnel oldTunnel, LocalDateTime revertTime) {
        super(flowParameters, stack, cluster);
        this.oldTunnel = oldTunnel;
        this.revertTime = revertTime;
    }

    public Tunnel getOldTunnel() {
        return oldTunnel;
    }

    public LocalDateTime getRevertTime() {
        return revertTime;
    }
}
