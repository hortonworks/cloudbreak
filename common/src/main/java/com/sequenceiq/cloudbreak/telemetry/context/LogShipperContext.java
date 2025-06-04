package com.sequenceiq.cloudbreak.telemetry.context;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sequenceiq.common.api.telemetry.model.SensitiveLoggingComponent;
import com.sequenceiq.common.api.telemetry.model.VmLog;

public class LogShipperContext {

    private final boolean enabled;

    private final boolean cloudStorageLogging;

    private final String cloudRegion;

    private final List<VmLog> vmLogs;

    private final Set<SensitiveLoggingComponent> enabledSensitiveStorageLogs;

    private LogShipperContext(Builder builder) {
        this.enabled = builder.enabled;
        this.cloudStorageLogging = builder.cloudStorageLogging;
        this.cloudRegion = builder.cloudRegion;
        this.vmLogs = builder.vmLogs;
        this.enabledSensitiveStorageLogs = builder.enabledSensitiveStorageLogs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isCloudStorageLogging() {
        return cloudStorageLogging;
    }

    public List<VmLog> getVmLogs() {
        return vmLogs;
    }

    public String getCloudRegion() {
        return cloudRegion;
    }

    public Set<SensitiveLoggingComponent> getEnabledSensitiveStorageLogs() {
        return enabledSensitiveStorageLogs;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "LogShipperContext{" +
                "enabled=" + enabled +
                ", cloudStorageLogging=" + cloudStorageLogging +
                ", cloudRegion='" + cloudRegion + '\'' +
                ", vmLogs=" + vmLogs +
                '}';
    }

    public static class Builder {

        private boolean enabled;

        private boolean cloudStorageLogging;

        private String cloudRegion;

        private List<VmLog> vmLogs;

        private Set<SensitiveLoggingComponent> enabledSensitiveStorageLogs = new HashSet<>();

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

        public Builder withCloudRegion(String cloudRegion) {
            this.cloudRegion = cloudRegion;
            return this;
        }

        public Builder withVmLogs(List<VmLog> vmLogs) {
            this.vmLogs = vmLogs;
            return this;
        }

        public Builder includeSaltLogsInCloudStorageLogs() {
            enabledSensitiveStorageLogs.add(SensitiveLoggingComponent.SALT);
            return this;
        }

    }
}
