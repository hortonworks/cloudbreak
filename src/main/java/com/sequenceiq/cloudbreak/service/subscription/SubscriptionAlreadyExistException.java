package com.sequenceiq.cloudbreak.service.subscription;

public class SubscriptionAlreadyExistException extends RuntimeException {

    public SubscriptionAlreadyExistException(String message) {
        super(message);
    }

}
