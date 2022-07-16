package com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class CreateResourcesResult extends CloudPlatformResult implements FlowPayload {

    public CreateResourcesResult(Long resourceId) {
        super(resourceId);
    }

    public CreateResourcesResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    @JsonCreator
    public CreateResourcesResult(
            @JsonProperty("status") EventStatus status,
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId) {
        super(status, statusReason, errorDetails, resourceId);
    }
}
