package com.sequenceiq.cloudbreak.auth.altus.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import io.netty.util.internal.StringUtil;

@Configuration
public class UmsConfig {

    @Value("${altus.ums.host:}")
    private String endpoint;

    @Value("${altus.ums.port:8982}")
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

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public static UmsConfig createConfig(String host, int port) {
        UmsConfig config = new UmsConfig();
        config.setEndpoint(host);
        config.setPort(port);
        return config;

    }
}
