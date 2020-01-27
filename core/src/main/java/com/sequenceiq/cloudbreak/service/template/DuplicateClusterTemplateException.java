package com.sequenceiq.cloudbreak.service.template;

import com.sequenceiq.cloudbreak.exception.BadRequestException;

public class DuplicateClusterTemplateException extends BadRequestException {

    public DuplicateClusterTemplateException(String message) {
        super(message);
    }

    public DuplicateClusterTemplateException(String message, Throwable cause) {
        super(message, cause);
    }

}
