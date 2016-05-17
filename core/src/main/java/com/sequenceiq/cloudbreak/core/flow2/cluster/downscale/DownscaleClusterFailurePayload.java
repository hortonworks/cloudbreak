package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterScaleFailedPayload;

public class DownscaleClusterFailurePayload extends ClusterScaleFailedPayload {

    public DownscaleClusterFailurePayload(Long stackId, String hostGroupName, Exception errorDetails) {
        super(stackId, hostGroupName, errorDetails);
    }
}
