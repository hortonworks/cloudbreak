package com.sequenceiq.cloudbreak.audit.config;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditConfig {

    private static final int DEFAULT_AUDIT_PORT = 80;

    @Value("${altus.audit.endpoint:}")
    private String endpoint;

    private String host;

    private int port;

    @PostConstruct
    public void init() {
        if (isConfigured()) {
            String[] parts = endpoint.split(":");
            if (parts.length < 1 || parts.length > 2) {
                throw new IllegalArgumentException("altus.audit.endpoint must be in host or host:port format.");
            }
            host = parts[0];
            port = parts.length == 2
                    ? Integer.parseInt(parts[1])
                    : DEFAULT_AUDIT_PORT;
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
