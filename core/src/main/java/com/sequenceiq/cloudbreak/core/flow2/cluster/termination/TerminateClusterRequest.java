package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import com.sequenceiq.cloudbreak.core.flow.context.AmbariClusterContext;
import com.sequenceiq.cloudbreak.core.flow.handlers.AmbariClusterRequest;

public class TerminateClusterRequest extends AmbariClusterRequest {

    public TerminateClusterRequest(AmbariClusterContext clusterContext) {
        super(clusterContext);
    }
}
