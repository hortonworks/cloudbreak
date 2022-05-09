package com.sequenceiq.cloudbreak.telemetry.monitoring;

import java.util.List;

public class NodeExporterConfiguration extends ExporterConfiguration {

    private List<String> collectors;

    public List<String> getCollectors() {
        return collectors;
    }

    public void setCollectors(List<String> collectors) {
        this.collectors = collectors;
    }
}
