package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterScaleFailedPayload;

public class UpscaleClusterFailedPayload extends ClusterScaleFailedPayload {

    public UpscaleClusterFailedPayload(Long stackId, String hostGroupName, Exception errorDetails) {
        super(stackId, hostGroupName, errorDetails);
    }
}
