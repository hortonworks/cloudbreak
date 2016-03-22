package com.sequenceiq.cloudbreak.cloud.template;

public class ResourceNotNeededException extends RuntimeException {

    public ResourceNotNeededException(String message) {
        super(message);
    }

}
