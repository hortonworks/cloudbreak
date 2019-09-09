package com.sequenceiq.cloudbreak.ccmimpl.altus.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import io.netty.util.internal.StringUtil;

@Configuration
public class MinaSshdManagementConfig {

    @Value("${altus.minasshdmgmt.host:}")
    private String endpoint;

    @Value("${altus.minasshdmgmt.port:8982}")
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
