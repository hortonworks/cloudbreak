package com.sequenceiq.cloudbreak.service.stack.flow;

import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

public class MetadataIncompleteException extends CloudbreakServiceException {

    public MetadataIncompleteException(String message) {
        super(message);
    }

}
