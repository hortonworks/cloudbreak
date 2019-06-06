package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class CreateCredentialResult extends CloudPlatformResult {

    public CreateCredentialResult(Long resourceId) {
        super(resourceId);
    }

    public CreateCredentialResult(Exception errorDetails, Long resourceId) {
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
