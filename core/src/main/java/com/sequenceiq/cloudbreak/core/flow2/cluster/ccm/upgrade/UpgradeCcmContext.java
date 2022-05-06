package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.core.FlowParameters;

public class UpgradeCcmContext extends ClusterViewContext {

    private final Tunnel oldTunnel;

    public UpgradeCcmContext(FlowParameters flowParameters, StackView stack, Tunnel oldTunnel) {
        super(flowParameters, stack);
        this.oldTunnel = oldTunnel;
    }

    public Tunnel getOldTunnel() {
        return oldTunnel;
    }
}
