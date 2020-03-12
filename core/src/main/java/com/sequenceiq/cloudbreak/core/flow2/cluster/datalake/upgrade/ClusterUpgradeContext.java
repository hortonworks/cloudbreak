package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class ClusterUpgradeContext extends CommonContext {

    private Long stackId;

    public ClusterUpgradeContext(FlowParameters flowParameters, StackEvent event) {
        super(flowParameters);
        stackId = event.getResourceId();
    }

    public ClusterUpgradeContext(FlowParameters flowParameters, Long stackId) {
        super(flowParameters);
        this.stackId = stackId;
    }

    public static ClusterUpgradeContext from(FlowParameters flowParameters, StackEvent event) {
        return new ClusterUpgradeContext(flowParameters, event);
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }
}
