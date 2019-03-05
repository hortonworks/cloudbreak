package com.sequenceiq.cloudbreak.cloud.event.validation;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class FileSystemValidationResult<R extends FileSystemValidationRequest> extends CloudPlatformResult<R> {

    public FileSystemValidationResult(R request) {
        super(request);
    }

    public FileSystemValidationResult(String statusReason, Exception errorDetails, R request) {
        super(statusReason, errorDetails, request);
    }
}
