package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.atlas;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class CheckAtlasUpdatedSaltContext extends CommonContext {
    private final Long stackId;

    public CheckAtlasUpdatedSaltContext(FlowParameters flowParameters, StackEvent event) {
        super(flowParameters);
        stackId = event.getResourceId();
    }

    public Long getStackId() {
        return stackId;
    }
}
