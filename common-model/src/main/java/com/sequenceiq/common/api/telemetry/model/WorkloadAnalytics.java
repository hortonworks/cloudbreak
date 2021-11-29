package com.sequenceiq.common.api.telemetry.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkloadAnalytics implements Serializable {

    @JsonProperty("databusEndpoint")
    private String databusEndpoint;

    @JsonProperty("attributes")
    private Map<String, Object> attributes = new HashMap<>();

    public String getDatabusEndpoint() {
        return databusEndpoint;
    }

    public void setDatabusEndpoint(String databusEndpoint) {
        this.databusEndpoint = databusEndpoint;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", WorkloadAnalytics.class.getSimpleName() + "[", "]")
                .add("databusEndpoint='" + databusEndpoint + '\'')
                .add("attributes='" + attributes + "'")
                .toString();
    }
}
