package com.sequenceiq.cloudbreak.orchestrator.model;

public class BootstrapParams {
    private String cloud;

    private String os;

    public String getCloud() {
        return cloud;
    }

    public void setCloud(String cloud) {
        this.cloud = cloud;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }
}
