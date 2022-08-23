package com.sequenceiq.cloudbreak.telemetry.monitoring;

public class BlackboxExporterConfiguration extends ExporterConfiguration {

    private Integer clouderaIntervalSeconds;

    private Integer cloudIntervalSeconds;

    private boolean checkOnAllNodes;

    public Integer getClouderaIntervalSeconds() {
        return clouderaIntervalSeconds;
    }

    public void setClouderaIntervalSeconds(Integer clouderaIntervalSeconds) {
        this.clouderaIntervalSeconds = clouderaIntervalSeconds;
    }

    public Integer getCloudIntervalSeconds() {
        return cloudIntervalSeconds;
    }

    public void setCloudIntervalSeconds(Integer cloudIntervalSeconds) {
        this.cloudIntervalSeconds = cloudIntervalSeconds;
    }

    public boolean isCheckOnAllNodes() {
        return checkOnAllNodes;
    }

    public void setCheckOnAllNodes(boolean checkOnAllNodes) {
        this.checkOnAllNodes = checkOnAllNodes;
    }
}
