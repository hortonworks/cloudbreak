package com.sequenceiq.common.api.node.status.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum HealthStatus {
    @JsonProperty("UNKNOWN") UKNOWN("UNKNOWN"),
    @JsonProperty("OK") OK("OK"),
    @JsonProperty("NOK") NOK("NOK");

    private String value;

    HealthStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "HealthStatus{" +
                "value='" + value + '\'' +
                '}';
    }
}
