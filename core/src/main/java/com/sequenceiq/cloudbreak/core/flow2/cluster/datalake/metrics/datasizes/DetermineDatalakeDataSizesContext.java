package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.metrics.datasizes;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class DetermineDatalakeDataSizesContext extends CommonContext {
    private final Long stackId;

    public DetermineDatalakeDataSizesContext(FlowParameters flowParameters, StackEvent event) {
        super(flowParameters);
        stackId = event.getResourceId();
    }

    public Long getStackId() {
        return stackId;
    }
}
