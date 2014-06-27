package com.sequenceiq.cloudbreak.service.stack;

import com.sequenceiq.cloudbreak.controller.InternalServerException;

public class NodeStartTimedOutException extends InternalServerException {

    public NodeStartTimedOutException(String message) {
        super(message);
    }

}
