package com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.proxy;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyDetails {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyDetails.class);

    private final boolean enabled;

    private String protocol;

    private String authentication;

    public ProxyDetails(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getAuthentication() {
        return authentication;
    }

    public static class Builder {

        private boolean enabled;

        private String protocol;

        private String authentication;

        public static Builder builder() {
            return new Builder();
        }

        public Builder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder withProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder withAuthentication(boolean auth) {
            this.authentication = auth ? "BASIC" : "NONE";
            return this;
        }

        public ProxyDetails build() {
            ProxyDetails proxyDetails = new ProxyDetails(enabled);
            proxyDetails.protocol = Optional.ofNullable(protocol).orElse("");
            proxyDetails.authentication = Optional.ofNullable(authentication).orElse("");
            return proxyDetails;
        }
    }
}
