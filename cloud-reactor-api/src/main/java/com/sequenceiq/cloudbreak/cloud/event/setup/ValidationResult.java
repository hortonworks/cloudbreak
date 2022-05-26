package com.sequenceiq.cloudbreak.cloud.event.setup;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class ValidationResult extends CloudPlatformResult {

    private Set<String> warningMessages;

    public ValidationResult(Long resourceId) {
        super(resourceId);
    }

    public ValidationResult(Long resourceId, Set<String> warningMessages) {
        super(resourceId);
        this.warningMessages = warningMessages;
    }

    public ValidationResult(Exception errorDetails, Long resourceId) {
        this(errorDetails.getMessage(), errorDetails, resourceId);
    }

    public ValidationResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public Set<String> getWarningMessages() {
        return warningMessages;
    }
}
