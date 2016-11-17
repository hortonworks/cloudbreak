package com.sequenceiq.periscope.subscription;


public class SubscriptionAlreadyExistException extends RuntimeException {

    public SubscriptionAlreadyExistException(String message) {
        super(message);
    }

}
