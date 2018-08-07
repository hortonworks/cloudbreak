package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class CreateCredentialResult extends CloudPlatformResult<CloudPlatformRequest<?>> {

    public CreateCredentialResult(CloudPlatformRequest<?> request) {
        super(request);
    }

    public CreateCredentialResult(Exception errorDetails, CloudPlatformRequest<?> request) {
        super("", errorDetails, request);
    }

    @Override
    public String toString() {
        return "CreateCredentialResult{"
                + "status=" + getStatus()
                + ", statusReason='" + getStatusReason() + '\''
                + ", errorDetails=" + getErrorDetails()
                + ", request=" + getRequest()
                + '}';
    }
}
