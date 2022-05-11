package com.sequenceiq.cloudbreak.authdistributor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthDistributorConfig {

    @Value("${authdistributor.host:}")
    private String endpoint;

    @Value("${authdistributor.port:8982}")
    private int port;

    public String getEndpoint() {
        return endpoint;
    }

    public int getPort() {
        return port;
    }

}