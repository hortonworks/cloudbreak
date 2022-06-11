package com.sequenceiq.cloudbreak.telemetry.monitoring;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.telemetry.TelemetryConfigView;

public class RequestSignerConfigView implements TelemetryConfigView {

    private final boolean enabled;

    private final Integer port;

    private final String user;

    private final boolean useToken;

    private final Integer tokenValidityMin;

    private RequestSignerConfigView(Builder builder) {
        this.enabled = builder.enabled;
        this.port = builder.port;
        this.user = builder.user;
        this.useToken = builder.useToken;
        this.tokenValidityMin = builder.tokenValidityMin;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        String prefix = "requestSigner";
        map.put(prefix + "Enabled", this.enabled);
        map.put(prefix + "Port", this.port);
        map.put(prefix + "User", this.user);
        map.put(prefix + "UseToken", this.useToken);
        map.put(prefix + "TokenValidityMin", this.tokenValidityMin);
        return map;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Integer getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public boolean isUseToken() {
        return useToken;
    }

    public Integer getTokenValidityMin() {
        return tokenValidityMin;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "RequestSignerConfigView{" +
                "enabled=" + enabled +
                ", port=" + port +
                ", user='" + user + '\'' +
                ", useToken=" + useToken +
                ", tokenValidityMin=" + tokenValidityMin +
                '}';
    }

    public static final class Builder {

        private boolean enabled;

        private Integer port;

        private String user;

        private boolean useToken;

        private Integer tokenValidityMin;

        private Builder() {
        }

        public RequestSignerConfigView build() {
            return new RequestSignerConfigView(this);
        }

        public Builder withPort(Integer port) {
            this.port = port;
            return this;
        }

        public Builder withUser(String user) {
            this.user = user;
            return this;
        }

        public Builder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder withUseToken(boolean useToken) {
            this.useToken = useToken;
            return this;
        }

        public Builder withTokenValidityMin(Integer tokenValidityMin) {
            this.tokenValidityMin = tokenValidityMin;
            return this;
        }
    }
}
