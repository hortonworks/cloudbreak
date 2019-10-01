package com.sequenceiq.environment.environment.dto.telemetry;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentTelemetry implements Serializable {

    private final EnvironmentLogging logging;

    private final Features features;

    private final Map<String, Object> fluentAttributes;

    private final String databusEndpoint;

    public EnvironmentTelemetry(@JsonProperty("logging") EnvironmentLogging logging,
            @JsonProperty("features") Features features,
            @JsonProperty("fluentAttributes") Map<String, Object> fluentAttributes,
            @JsonProperty("databusEndpoint") String databusEndpoint) {
        this.logging = logging;
        this.features = features;
        this.fluentAttributes = fluentAttributes;
        this.databusEndpoint = databusEndpoint;
    }

    public EnvironmentLogging getLogging() {
        return logging;
    }

    public Features getFeatures() {
        return features;
    }

    public Map<String, Object> getFluentAttributes() {
        return fluentAttributes;
    }

    public String getDatabusEndpoint() {
        return databusEndpoint;
    }
}