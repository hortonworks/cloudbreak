package com.sequenceiq.cloudbreak.cm.client.transaction;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class CmApiCallInTransactionException extends CloudbreakServiceException {
    public CmApiCallInTransactionException(String message) {
        super(message);
    }

    public CmApiCallInTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public CmApiCallInTransactionException(Throwable cause) {
        super(cause);
    }
}
