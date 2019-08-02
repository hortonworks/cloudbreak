package com.sequenceiq.cloudbreak.telemetry.metering;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.telemetry.TelemetryConfigView;

public class MeteringConfigView implements TelemetryConfigView {

    private final boolean enabled;

    private final String clusterCrn;

    private final String serviceType;

    private final String serviceVersion;

    private final String platform;

    private MeteringConfigView(Builder builder) {
        this.enabled = builder.enabled;
        this.clusterCrn = builder.clusterCrn;
        this.serviceType = builder.serviceType;
        this.serviceVersion = builder.serviceVersion;
        this.platform = builder.platform;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getClusterCrn() {
        return clusterCrn;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public String getPlatform() {
        return platform;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("enabled", this.enabled);
        map.put("clusterCrn", this.clusterCrn);
        map.put("serviceType", this.serviceType);
        map.put("serviceVersion", this.serviceVersion);
        map.put("platform", this.platform);
        return map;
    }

    public static final class Builder {

        private boolean enabled;

        private String clusterCrn;

        private String serviceType;

        private String serviceVersion;

        private String platform;

        public MeteringConfigView build() {
            return new MeteringConfigView(this);
        }

        public MeteringConfigView.Builder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public MeteringConfigView.Builder withClusterCrn(String clusterCrn) {
            this.clusterCrn = clusterCrn;
            return this;
        }

        public MeteringConfigView.Builder withServiceType(String serviceType) {
            this.serviceType = serviceType;
            return this;
        }

        public MeteringConfigView.Builder withServiceVersion(String serviceVersion) {
            this.serviceVersion = serviceVersion;
            return this;
        }

        public Builder withPlatform(String platform) {
            this.platform = platform;
            return this;
        }
    }
}
