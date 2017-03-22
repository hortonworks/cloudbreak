package com.sequenceiq.cloudbreak.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ecwid.consul.v1.ConsulClient;

@Configuration
public class LocalConsulConfig {

    private static final int DEFAULT_TIMEOUT_MS = 5000;

    @Value("${spring.cloud.consul.host:consul.service.consul}")
    private String consulHost;

    @Value("${spring.cloud.consul.port:8500}")
    private int consulPort;

    @Bean
    public ConsulClient consulClient() {
        return new ConsulClient(consulHost, consulPort, DEFAULT_TIMEOUT_MS);
    }

}
