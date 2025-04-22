package com.sequenceiq.cloudbreak.telemetry.databus;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import com.sequenceiq.cloudbreak.telemetry.TelemetryConfigView;

public class DatabusConfigView implements TelemetryConfigView {

    private static final String DBUS_ACCESS_KEY_SECRET_ALGORITHM_DEFAULT = "Ed25519";

    private final boolean enabled;

    private final String endpoint;

    private final String accessKeyId;

    private final char[] accessKeySecret;

    private final String accessKeySecretAlgorithm;

    private DatabusConfigView(Builder builder) {
        this.enabled = builder.enabled;
        this.endpoint = builder.endpoint;
        this.accessKeyId = builder.accessKeyId;
        this.accessKeySecret = builder.accessKeySecret;
        this.accessKeySecretAlgorithm = builder.accessKeySecretAlgorithm;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public char[] getAccessKeySecret() {
        return accessKeySecret;
    }

    public String getAccessKeySecretAlgorithm() {
        return accessKeySecretAlgorithm;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("enabled", this.enabled);
        map.put("endpoint", this.endpoint);
        map.put("accessKeyId", this.accessKeyId);
        map.put("accessKeySecret", this.accessKeySecret != null
                ? new String(this.accessKeySecret) : "");
        map.put("accessKeySecretAlgorithm", ObjectUtils.defaultIfNull(
                this.accessKeySecretAlgorithm, DBUS_ACCESS_KEY_SECRET_ALGORITHM_DEFAULT));
        return map;
    }

    public static final class Builder {

        private boolean enabled;

        private String endpoint;

        private String accessKeyId;

        private char[] accessKeySecret;

        private String accessKeySecretAlgorithm;

        public DatabusConfigView build() {
            return new DatabusConfigView(this);
        }

        public Builder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder withEnabled() {
            this.enabled = true;
            return this;
        }

        public Builder withEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder withAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
            return this;
        }

        public Builder withAccessKeySecret(char[] accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
            return this;
        }

        public Builder withAccessKeySecretAlgorithm(String accessKeySecretAlgorithm) {
            this.accessKeySecretAlgorithm = accessKeySecretAlgorithm;
            return this;
        }
    }
}
