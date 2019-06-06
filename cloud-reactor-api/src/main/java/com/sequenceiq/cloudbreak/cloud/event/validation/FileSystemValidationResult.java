package com.sequenceiq.cloudbreak.cloud.event.validation;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class FileSystemValidationResult extends CloudPlatformResult {

    public FileSystemValidationResult(Long resourceId) {
        super(resourceId);
    }

    public FileSystemValidationResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }
}
