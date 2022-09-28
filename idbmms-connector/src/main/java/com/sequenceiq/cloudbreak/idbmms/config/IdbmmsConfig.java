package com.sequenceiq.cloudbreak.idbmms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import io.netty.util.internal.StringUtil;

/**
 * Configuration settings for the IDBroker Mapping Management Service endpoint.
 */
@Configuration
public class IdbmmsConfig {

    @Value("${altus.idbmms.host:}")
    private String endpoint;

    @Value("${altus.idbmms.port:8990}")
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
