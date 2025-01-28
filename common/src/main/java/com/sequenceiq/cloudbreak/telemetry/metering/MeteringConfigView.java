package com.sequenceiq.cloudbreak.telemetry.metering;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.telemetry.TelemetryConfigView;

public class MeteringConfigView implements TelemetryConfigView {

    private final boolean enabled;

    private final String clusterCrn;

    private final String clusterName;

    private final String serviceType;

    private final String serviceVersion;

    private final String streamName;

    private final String platform;

    private final String desiredMeteringAgentDate;

    private MeteringConfigView(Builder builder) {
        this.enabled = builder.enabled;
        this.clusterCrn = builder.clusterCrn;
        this.clusterName = builder.clusterName;
        this.serviceType = builder.serviceType;
        this.serviceVersion = builder.serviceVersion;
        this.streamName = builder.streamName;
        this.platform = builder.platform;
        this.desiredMeteringAgentDate = builder.desiredMeteringAgentDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getClusterCrn() {
        return clusterCrn;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public String getStreamName() {
        return streamName;
    }

    public String getPlatform() {
        return platform;
    }

    public String getDesiredMeteringAgentDate() {
        return desiredMeteringAgentDate;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("enabled", this.enabled);
        map.put("clusterCrn", this.clusterCrn);
        map.put("clusterName", this.clusterName);
        map.put("serviceType", this.serviceType);
        map.put("serviceVersion", this.serviceVersion);
        map.put("streamName", this.streamName);
        map.put("platform", this.platform);
        map.put("desiredMeteringAgentDate", this.desiredMeteringAgentDate);
        return map;
    }

    public static final class Builder {

        private boolean enabled;

        private String clusterCrn;

        private String clusterName;

        private String serviceType;

        private String serviceVersion;

        private String streamName;

        private String platform;

        private String desiredMeteringAgentDate;

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

        public MeteringConfigView.Builder withClusterName(String clusterName) {
            this.clusterName = clusterName;
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

        public MeteringConfigView.Builder withStreamName(String streamName) {
            this.streamName = streamName;
            return this;
        }

        public Builder withPlatform(String platform) {
            this.platform = platform;
            return this;
        }

        public MeteringConfigView.Builder withDesiredMeteringAgentDate(String desiredMeteringAgentDate) {
            this.desiredMeteringAgentDate = desiredMeteringAgentDate;
            return this;
        }
    }
}
