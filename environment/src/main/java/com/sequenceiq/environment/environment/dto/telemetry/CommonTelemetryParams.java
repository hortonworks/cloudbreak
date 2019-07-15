package com.sequenceiq.environment.environment.dto.telemetry;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class CommonTelemetryParams implements Serializable {

    private Map<String, Object> attributes = new HashMap<>();

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
