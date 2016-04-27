package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.reactor.api.event.ClusterUpscalePayload;

public class UpscaleClusterFailedPayload implements ClusterUpscalePayload {

    private final Long stackId;
    private final String hostGroupName;
    private final Exception errorDetails;

    public UpscaleClusterFailedPayload(Long stackId, String hostGroupName, Exception errorDetails) {
        this.stackId = stackId;
        this.hostGroupName = hostGroupName;
        this.errorDetails = errorDetails;
    }

    @Override
    public Long getStackId() {
        return stackId;
    }

    @Override
    public String getHostGroupName() {
        return hostGroupName;
    }

    public Exception getErrorDetails() {
        return errorDetails;
    }
}
