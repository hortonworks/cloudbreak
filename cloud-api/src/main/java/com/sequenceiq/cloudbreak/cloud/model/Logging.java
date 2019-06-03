package com.sequenceiq.cloudbreak.cloud.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Logging {

    private final boolean enabled;

    private final LoggingOutputType outputType;

    private final Map<String, Object> attributes;

    public Logging(@JsonProperty("enabled") boolean enabled,
            @JsonProperty("output") LoggingOutputType outputType,
            @JsonProperty("attributes") Map<String, Object> attributes) {
        this.enabled = enabled;
        this.outputType = outputType;
        this.attributes = attributes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LoggingOutputType getOutputType() {
        return outputType;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
