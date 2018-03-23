package com.sequenceiq.cloudbreak.service.smartsense;

import com.sequenceiq.cloudbreak.controller.BadRequestException;

public class SmartSenseSubscriptionAccessDeniedException extends BadRequestException {

    public SmartSenseSubscriptionAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }

    public SmartSenseSubscriptionAccessDeniedException(String message) {
        super(message);
    }

}
