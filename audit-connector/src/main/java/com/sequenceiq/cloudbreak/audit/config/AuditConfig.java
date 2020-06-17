package com.sequenceiq.cloudbreak.audit.config;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditConfig {

    @Value("${altus.audit.endpoint:}")
    private String endpoint;

    private String host;

    private int port;

    @PostConstruct
    public void init() {
        if (isConfigured()) {
            String[] parts = endpoint.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("altus.audit.endpoint must be in host:port format.");
            }
            host = parts[0];
            port = Integer.parseInt(parts[1]);
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isConfigured() {
        return StringUtils.isNotBlank(endpoint);
    }
}
