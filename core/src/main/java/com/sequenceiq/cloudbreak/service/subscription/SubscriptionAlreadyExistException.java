package com.sequenceiq.cloudbreak.service.subscription;

import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

public class SubscriptionAlreadyExistException extends CloudbreakServiceException {

    public SubscriptionAlreadyExistException(String message) {
        super(message);
    }

}
