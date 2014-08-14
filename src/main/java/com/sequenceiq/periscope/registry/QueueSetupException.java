package com.sequenceiq.periscope.registry;

import java.util.Map;

public class QueueSetupException extends Exception {

    private final Map<String, String> properties;

    public QueueSetupException(String message, Map<String, String> properties) {
        super(message);
        this.properties = properties;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
