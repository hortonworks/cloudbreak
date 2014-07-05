package com.sequenceiq.cloudbreak.service.stack;

import com.sequenceiq.cloudbreak.controller.InternalServerException;

public class MetadataIncompleteException extends InternalServerException {

    public MetadataIncompleteException(String message) {
        super(message);
    }

}
