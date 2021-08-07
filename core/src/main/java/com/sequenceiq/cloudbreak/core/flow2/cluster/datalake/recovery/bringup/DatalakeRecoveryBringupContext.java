package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class DatalakeRecoveryBringupContext extends CommonContext {

    private Long stackId;

    public DatalakeRecoveryBringupContext(FlowParameters flowParameters, StackEvent event) {
        super(flowParameters);
        stackId = event.getResourceId();
    }

    public static DatalakeRecoveryBringupContext from(FlowParameters flowParameters, StackEvent event) {
        return new DatalakeRecoveryBringupContext(flowParameters, event);
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }
}
