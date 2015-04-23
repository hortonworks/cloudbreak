package com.sequenceiq.cloudbreak.service.cluster;

public class PluginFailureException extends RuntimeException {

    public PluginFailureException(String message) {
        super(message);
    }

}
