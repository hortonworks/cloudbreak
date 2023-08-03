package com.sequenceiq.freeipa.flow.freeipa.upscale.event;

import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

public class UpscaleStackImageFallbackResult extends UpscaleStackResult {

    @JsonCreator
    public UpscaleStackImageFallbackResult(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceStatus") ResourceStatus resourceStatus,
            @JsonProperty("results") List<CloudResourceStatus> results) {

        super(resourceId, resourceStatus, results);
    }

    public UpscaleStackImageFallbackResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", UpscaleStackImageFallbackResult.class.getSimpleName() + "[", "]")
                .add("resourceStatus=" + getResourceStatus())
                .add("results=" + getResults())
                .toString();
    }

}
