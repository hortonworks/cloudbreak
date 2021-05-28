package com.sequenceiq.cloudbreak.sigmadbus.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SigmaDatabusConfig {

    private static final int DEFAULT_SIGMA_DBUS_PORT = 8982;

    private final String endpoint;

    private final String host;

    private final Integer port;

    public SigmaDatabusConfig(@Value("${altus.sigmadbus.endpoint:}") String endpoint) {
        this.endpoint = endpoint;
        if (StringUtils.isNotBlank(this.endpoint)) {
            String[] parts = endpoint.split(":");
            if (parts.length < 1 || parts.length > 2) {
                throw new IllegalArgumentException("altus.sigmadbus.endpoint must be in host or host:port format.");
            }
            host = parts[0];
            port = parts.length == 2
                    ? Integer.parseInt(parts[1])
                    : DEFAULT_SIGMA_DBUS_PORT;
        } else {
            host = null;
            port = null;
        }
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }
}
