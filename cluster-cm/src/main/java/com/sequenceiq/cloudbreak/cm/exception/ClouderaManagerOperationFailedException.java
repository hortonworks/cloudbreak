package com.sequenceiq.cloudbreak.cm.exception;

import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.cm.util.ClouderaManagerApiExceptionUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class ClouderaManagerOperationFailedException extends CloudbreakServiceException {

    public ClouderaManagerOperationFailedException(String message) {
        super(message);
    }

    public ClouderaManagerOperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Wraps a Cloudera Manager {@link ApiException}, using the human-readable detail from its response body as the message. Callers do not need to know how CM
     * encodes that detail; see {@link ClouderaManagerApiExceptionUtil#extractMessage(ApiException)}.
     */
    public ClouderaManagerOperationFailedException(ApiException apiException) {
        super(ClouderaManagerApiExceptionUtil.extractMessage(apiException), apiException);
    }
}
