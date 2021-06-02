package com.sequenceiq.cloudbreak.cloud.aws.common.exception;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class AwsConfusedDeputyException extends CloudbreakServiceException {
    public AwsConfusedDeputyException(String message) {
        super(message);
    }

    public AwsConfusedDeputyException(String message, Throwable cause) {
        super(message, cause);
    }

    public AwsConfusedDeputyException(Throwable cause) {
        super(cause);
    }
}
