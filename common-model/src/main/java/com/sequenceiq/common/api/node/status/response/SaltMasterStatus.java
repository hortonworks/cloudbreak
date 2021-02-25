package com.sequenceiq.common.api.node.status.response;

public class SaltMasterStatus {

    private HealthStatus saltApi;

    private HealthStatus saltBootstrap;

    private HealthStatus saltMaster;

    private Long timestamp;

    public HealthStatus getSaltApi() {
        return saltApi;
    }

    public void setSaltApi(HealthStatus saltApi) {
        this.saltApi = saltApi;
    }

    public HealthStatus getSaltBootstrap() {
        return saltBootstrap;
    }

    public void setSaltBootstrap(HealthStatus saltBootstrap) {
        this.saltBootstrap = saltBootstrap;
    }

    public HealthStatus getSaltMaster() {
        return saltMaster;
    }

    public void setSaltMaster(HealthStatus saltMaster) {
        this.saltMaster = saltMaster;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "SaltMasterStatus{" +
                "saltApi=" + saltApi +
                ", saltBootstrap=" + saltBootstrap +
                ", saltMaster=" + saltMaster +
                ", timestamp=" + timestamp +
                '}';
    }
}
