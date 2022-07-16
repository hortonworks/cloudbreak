package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class CreateCredentialResult extends CloudPlatformResult implements FlowPayload {

    public CreateCredentialResult(Long resourceId) {
        super(resourceId);
    }

    @JsonCreator
    public CreateCredentialResult(
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId) {
        super("", errorDetails, resourceId);
    }

    @Override
    public String toString() {
        return "CreateCredentialResult{"
                + "status=" + getStatus()
                + ", statusReason='" + getStatusReason() + '\''
                + ", errorDetails=" + getErrorDetails()
                + '}';
    }
}
