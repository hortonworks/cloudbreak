package com.sequenceiq.cloudbreak.cost.banzai;

public class BanzaiOperationFailedException extends RuntimeException {

    public BanzaiOperationFailedException(String message) {
        super(message);
    }

    public BanzaiOperationFailedException(Throwable cause) {
        super(cause);
    }

    public BanzaiOperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
