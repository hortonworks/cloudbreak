package com.sequenceiq.cloudbreak.cloud.event.validation;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class FileSystemValidationResult extends CloudPlatformResult {

    public FileSystemValidationResult(CloudPlatformRequest request) {
        super(request);
    }

    public FileSystemValidationResult(String statusReason, Exception errorDetails, CloudPlatformRequest request) {
        super(statusReason, errorDetails, request);
    }
}
