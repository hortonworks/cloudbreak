package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.metrics.datasizes;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.datalakemetrics.datasizes.DetermineDatalakeDataSizesBaseEvent;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class DetermineDatalakeDataSizesContext extends CommonContext {
    private final Long stackId;

    private final String operationId;

    public DetermineDatalakeDataSizesContext(FlowParameters flowParameters, DetermineDatalakeDataSizesBaseEvent event) {
        super(flowParameters);
        stackId = event.getResourceId();
        operationId = event.getOperationId();
    }

    public Long getStackId() {
        return stackId;
    }

    public String getOperationId() {
        return operationId;
    }
}
