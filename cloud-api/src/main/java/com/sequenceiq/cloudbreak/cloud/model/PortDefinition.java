package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PortDefinition {

    private final String from;

    private final String to;

    @JsonCreator
    public PortDefinition(
            @JsonProperty("from") String from,
            @JsonProperty("to") String to) {

        this.to = to;
        this.from = from;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public boolean isRange() {
        return !from.equals(to);
    }

    @Override
    public String toString() {
        return "PortDefinition{" +  "from='" + from + '\'' + ", to='" + to + '\'' + '}';
    }
}
