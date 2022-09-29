package com.sequenceiq.cloudbreak.datalakedr.config;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatalakeDrConfig {

    static final int DEFAULT_DATALAKE_DR_PORT = 80;

    @Value("${altus.datalakedr.endpoint}")
    private String endpoint;

    @Value("${altus.datalakedr.enabled}")
    private boolean enabled;

    private String host;

    private int port;

    @PostConstruct
    public void init() {
        if (enabled) {
            if (isConfigured()) {
                String[] parts = endpoint.split(":");
                if (parts.length < 1 || parts.length > 2) {
                    throw new IllegalArgumentException("altus.datalakedr.endpoint must be in host or host:port format.");
                }
                host = parts[0];
                port = parts.length == 2
                    ? Integer.parseInt(parts[1])
                    : DEFAULT_DATALAKE_DR_PORT;
            } else {
                throw new IllegalStateException("altus.datalakedr.endpoint is not configured");
            }
        }
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isConfigured() {
        return enabled && StringUtils.isNotBlank(endpoint);
    }
}
