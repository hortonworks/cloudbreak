package com.sequenceiq.cloudbreak.saas.client.sdx.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import io.netty.util.internal.StringUtil;

@Configuration
public class SdxSaasChannelConfig {

    @Value("${saas.sdx.host:localhost}")
    private String endpoint;

    @Value("${saas.sdx.port:8982}")
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
