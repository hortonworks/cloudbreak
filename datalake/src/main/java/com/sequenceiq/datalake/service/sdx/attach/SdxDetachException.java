package com.sequenceiq.datalake.service.sdx.attach;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class SdxDetachException extends CloudbreakServiceException {
    SdxDetachException(String message) {
        super(message);
    }
}
