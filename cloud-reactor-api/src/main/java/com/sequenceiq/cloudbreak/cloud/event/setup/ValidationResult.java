package com.sequenceiq.cloudbreak.cloud.event.setup;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class ValidationResult extends CloudPlatformResult implements FlowPayload {

    private final Set<String> warningMessages;

    public ValidationResult(Long resourceId) {
        super(resourceId);
        this.warningMessages = null;
    }

    public ValidationResult(Long resourceId, Set<String> warningMessages) {
        super(resourceId);
        this.warningMessages = warningMessages;
    }

    public ValidationResult(Exception errorDetails, Long resourceId) {
        this(errorDetails.getMessage(), errorDetails, resourceId);
    }

    @JsonCreator
    public ValidationResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId) {
        super(statusReason, errorDetails, resourceId);
        this.warningMessages = null;
    }

    public Set<String> getWarningMessages() {
        return warningMessages;
    }
}
