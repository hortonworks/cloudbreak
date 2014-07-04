package com.sequenceiq.cloudbreak.service.stack;

import com.sequenceiq.cloudbreak.controller.InternalServerException;

public class WrongMetadataException extends InternalServerException {

    public WrongMetadataException(String message) {
        super(message);
    }

}
