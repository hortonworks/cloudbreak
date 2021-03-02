package com.sequenceiq.cloudbreak.cm.exception;

import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;

public class CloudStorageConfigurationFailedException extends ClouderaManagerOperationFailedException {

    public CloudStorageConfigurationFailedException(String message) {
        super(message);
    }
}
