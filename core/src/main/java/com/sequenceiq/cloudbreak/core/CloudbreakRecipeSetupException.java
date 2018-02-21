package com.sequenceiq.cloudbreak.core;

import com.sequenceiq.cloudbreak.service.CloudbreakException;

public class CloudbreakRecipeSetupException extends CloudbreakException {
    public CloudbreakRecipeSetupException(String message) {
        super(message);
    }

    public CloudbreakRecipeSetupException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudbreakRecipeSetupException(Throwable cause) {
        super(cause);
    }
}
