package com.sequenceiq.cloudbreak.telemetry.monitoring;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfigView;

public class MonitoringConfigView implements TelemetryConfigView {

    private static final String EMPTY_CONFIG_DEFAULT = "";

    private final boolean enabled;

    private final String type;

    private final String cmUsername;

    private final char[] cmPassword;

    private final TelemetryClusterDetails clusterDetails;

    private MonitoringConfigView(Builder builder) {
        this.enabled = builder.enabled;
        this.type = builder.type;
        this.cmUsername = builder.cmUsername;
        this.cmPassword = builder.cmPassword;
        this.clusterDetails = builder.clusterDetails;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getCmUsername() {
        return cmUsername;
    }

    public char[] getCmPassword() {
        return cmPassword;
    }

    public String getType() {
        return type;
    }

    public TelemetryClusterDetails getClusterDetails() {
        return clusterDetails;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("enabled", this.enabled);
        map.put("type", ObjectUtils.defaultIfNull(this.type, EMPTY_CONFIG_DEFAULT));
        map.put("cmUsername", ObjectUtils.defaultIfNull(this.cmUsername, EMPTY_CONFIG_DEFAULT));
        map.put("cmPassword", ObjectUtils.defaultIfNull(this.cmPassword, EMPTY_CONFIG_DEFAULT));
        if (this.clusterDetails != null) {
            map.putAll(clusterDetails.toMap());
        }
        return map;
    }

    public static final class Builder {

        private boolean enabled;

        private String type;

        private String cmUsername;

        private char[] cmPassword;

        private TelemetryClusterDetails clusterDetails;

        public MonitoringConfigView build() {
            return new MonitoringConfigView(this);
        }

        public Builder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withCMUsername(String cmUsername) {
            this.cmUsername = cmUsername;
            return this;
        }

        public Builder withCMPassword(char[] cmPassword) {
            this.cmPassword = cmPassword;
            return this;
        }

        public Builder withClusterDetails(TelemetryClusterDetails clusterDetails) {
            this.clusterDetails = clusterDetails;
            return this;
        }
    }
}
