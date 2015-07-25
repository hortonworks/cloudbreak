package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

public class AwsResourceException extends CloudConnectorException {

    public AwsResourceException(String message) {
        super(message);
    }

    public AwsResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
