package com.sequenceiq.cloudbreak.cloud.model;

public class PortDefinition {

    private final String from;

    private final String to;

    public PortDefinition(String from, String to) {
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
