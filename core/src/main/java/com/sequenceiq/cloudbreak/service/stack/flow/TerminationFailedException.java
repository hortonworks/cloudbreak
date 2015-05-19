package com.sequenceiq.cloudbreak.service.stack.flow;

import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

public class TerminationFailedException extends CloudbreakServiceException {


    public TerminationFailedException(String message) {
        super(message);
    }

    public TerminationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public TerminationFailedException(Throwable cause) {
        super(cause);
    }


}
