package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import com.sequenceiq.cloudbreak.cloud.connector.CloudConnectorException;

public class AwsResourceException extends CloudConnectorException {

    public AwsResourceException(String message) {
        super(message);
    }
}
