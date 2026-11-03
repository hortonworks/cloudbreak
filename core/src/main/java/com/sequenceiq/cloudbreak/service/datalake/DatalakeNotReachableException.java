package com.sequenceiq.cloudbreak.service.datalake;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class DatalakeNotReachableException extends CloudbreakServiceException {

    public DatalakeNotReachableException(String message, Throwable cause) {
        super(message, cause);
    }
}
