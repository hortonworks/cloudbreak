package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.ResourceType;

public class GccResourceCreationException extends InternalServerException {
    public GccResourceCreationException(String message) {
        super(message);
    }

    public GccResourceCreationException(String message, ResourceType resourceType, String name) {
        super(String.format("%s: exception occured on %s resource with %s resourceType", message, resourceType.name(), name));
    }
}
