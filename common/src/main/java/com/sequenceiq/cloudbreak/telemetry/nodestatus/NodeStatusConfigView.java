package com.sequenceiq.cloudbreak.telemetry.nodestatus;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import com.sequenceiq.cloudbreak.telemetry.TelemetryConfigView;

import io.micrometer.core.instrument.util.StringUtils;

public class NodeStatusConfigView implements TelemetryConfigView {

    private static final String EMPTY_CONFIG_DEFAULT = "";

    private final String serverUsername;

    private final char[] serverPassword;

    private NodeStatusConfigView(Builder builder) {
        this.serverUsername = builder.serverUsername;
        this.serverPassword = builder.serverPassword;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("serverUsername", ObjectUtils.defaultIfNull(this.serverUsername, EMPTY_CONFIG_DEFAULT));
        String serverSha256Password = EMPTY_CONFIG_DEFAULT;
        if (this.serverPassword != null && StringUtils.isNotBlank(new String(this.serverPassword))) {
            serverSha256Password = new String(this.serverPassword);
        }
        map.put("serverPassword", serverSha256Password);
        return map;
    }

    public static final class Builder {

        private String serverUsername;

        private char[] serverPassword;

        public NodeStatusConfigView build() {
            return new NodeStatusConfigView(this);
        }

        public Builder withServerUsername(String serverUsername) {
            this.serverUsername = serverUsername;
            return this;
        }

        public Builder withServerPassword(char[] serverPassword) {
            this.serverPassword = serverPassword;
            return this;
        }
    }
}
