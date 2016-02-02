package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import com.sequenceiq.cloudbreak.core.flow.handlers.AmbariClusterResult;

public class TerminateClusterResult extends AmbariClusterResult<TerminateClusterRequest> {

    public TerminateClusterResult(TerminateClusterRequest request) {
        super(request);
    }

}
