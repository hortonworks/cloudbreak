package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.cloud.event.Payload;

public class UpscaleClusterFailedPayload implements Payload {

    private final Long stackId;
    private final String errorReason;

    UpscaleClusterFailedPayload(Long stackId, String errorReason) {
        this.stackId = stackId;
        this.errorReason = errorReason;
    }

    @Override
    public Long getStackId() {
        return stackId;
    }

    public String getErrorReason() {
        return errorReason;
    }
}
