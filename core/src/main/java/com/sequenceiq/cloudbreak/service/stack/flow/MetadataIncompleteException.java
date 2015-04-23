package com.sequenceiq.cloudbreak.service.stack.flow;

import com.sequenceiq.cloudbreak.controller.InternalServerException;

public class MetadataIncompleteException extends InternalServerException {

    public MetadataIncompleteException(String message) {
        super(message);
    }

}
