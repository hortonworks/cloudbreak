package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.core.flow.context.DefaultFlowContext;

public class DefaultClusterFlowContext extends DefaultFlowContext{

    private Long clusterId;

    public DefaultClusterFlowContext(Long stackId, Platform cloudPlatform, Long clusterId) {
        super(stackId, cloudPlatform);
        this.clusterId = clusterId;
    }

    public DefaultClusterFlowContext(Long stackId, Platform cloudPlatform, String errorReason, Long clusterId) {
        super(stackId, cloudPlatform, errorReason);
        this.clusterId = clusterId;
    }

    public Long getClusterId() {
        return clusterId;
    }
}
