package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import com.sequenceiq.cloudbreak.controller.InternalServerException;

public class CloudFormationStackException extends InternalServerException {

    public CloudFormationStackException(String message) {
        super(message);
    }
}
