package com.sequenceiq.cloudbreak.telemetry.context;

import java.util.List;

import com.sequenceiq.common.api.telemetry.model.VmLog;

public class LogShipperContext {

    private final boolean enabled;

    private final boolean cloudStorageLogging;

    private final boolean collectDeploymentLogs;

    private final String cloudRegion;

    private final List<VmLog> vmLogs;

    private LogShipperContext(Builder builder) {
        this.enabled = builder.enabled;
        this.cloudStorageLogging = builder.cloudStorageLogging;
        this.collectDeploymentLogs = builder.collectDeploymentLogs;
        this.cloudRegion = builder.cloudRegion;
        this.vmLogs = builder.vmLogs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isCloudStorageLogging() {
        return cloudStorageLogging;
    }

    public boolean isCollectDeploymentLogs() {
        return collectDeploymentLogs;
    }

    public List<VmLog> getVmLogs() {
        return vmLogs;
    }

    public String getCloudRegion() {
        return cloudRegion;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "LogShipperContext{" +
                "enabled=" + enabled +
                ", cloudStorageLogging=" + cloudStorageLogging +
                ", collectDeploymentLogs=" + collectDeploymentLogs +
                ", cloudRegion='" + cloudRegion + '\'' +
                ", vmLogs=" + vmLogs +
                '}';
    }

    public static class Builder {

        private boolean enabled;

        private boolean cloudStorageLogging;

        private boolean collectDeploymentLogs;

        private String cloudRegion;

        private List<VmLog> vmLogs;

        private Builder() {
        }

        public LogShipperContext build() {
            return new LogShipperContext(this);
        }

        public Builder enabled() {
            this.enabled = true;
            return this;
        }

        public Builder cloudStorageLogging() {
            this.cloudStorageLogging = true;
            return this;
        }

        public Builder collectDeploymentLogs() {
            this.collectDeploymentLogs = true;
            return this;
        }

        public Builder withCloudRegion(String cloudRegion) {
            this.cloudRegion = cloudRegion;
            return this;
        }

        public Builder withVmLogs(List<VmLog> vmLogs) {
            this.vmLogs = vmLogs;
            return this;
        }

    }
}
