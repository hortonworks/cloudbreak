package com.sequenceiq.cloudbreak.audit.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import io.netty.util.internal.StringUtil;

@Configuration
public class AuditConfig {

    @Value("${altus.audit.host:}")
    private String endpoint;

    @Value("${altus.audit.port:8989}")
    private int port;

    public String getEndpoint() {
        return endpoint;
    }

    public int getPort() {
        return port;
    }

    public boolean isConfigured() {
        return !StringUtil.isNullOrEmpty(endpoint);
    }
}
