package com.sequenceiq.cloudbreak.telemetry.nodestatus;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import com.sequenceiq.cloudbreak.telemetry.TelemetryConfigView;

import io.micrometer.core.instrument.util.StringUtils;

public class NodeStatusConfigView implements TelemetryConfigView {

    private static final String EMPTY_CONFIG_DEFAULT = "";

    private final String serverUsername;

    private final String serverPassword;

    private final boolean saltPingEnabled;

    private NodeStatusConfigView(Builder builder) {
        this.serverUsername = builder.serverUsername;
        this.serverPassword = builder.serverPassword;
        this.saltPingEnabled = builder.saltPingEnabled;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("serverUsername", ObjectUtils.defaultIfNull(this.serverUsername, EMPTY_CONFIG_DEFAULT));
        String serverSha256Password = EMPTY_CONFIG_DEFAULT;
        if (this.serverPassword != null && StringUtils.isNotBlank(this.serverPassword)) {
            serverSha256Password = this.serverPassword;
        }
        map.put("serverPassword", serverSha256Password);
        map.put("saltPingEnabled", saltPingEnabled);
        return map;
    }

    public static final class Builder {

        private String serverUsername;

        private String serverPassword;

        private boolean saltPingEnabled;

        public NodeStatusConfigView build() {
            return new NodeStatusConfigView(this);
        }

        public Builder withServerUsername(String serverUsername) {
            this.serverUsername = serverUsername;
            return this;
        }

        public Builder withServerPassword(String serverPassword) {
            this.serverPassword = serverPassword;
            return this;
        }

        public Builder withSaltPingEnabled(boolean saltPingEnabled) {
            this.saltPingEnabled = saltPingEnabled;
            return this;
        }
    }
}
