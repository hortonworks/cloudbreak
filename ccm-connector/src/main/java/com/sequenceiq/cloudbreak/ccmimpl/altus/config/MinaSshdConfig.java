package com.sequenceiq.cloudbreak.ccmimpl.altus.config;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import io.netty.util.internal.StringUtil;

@Configuration
public class MinaSshdConfig {
    @Value("${altus.minasshd.host:}")
    private String host;

    @Value("${altus.minasshd.port:8982}")
    private int port;

    public Optional<String> getHost() {
        return StringUtil.isNullOrEmpty(host) ? Optional.empty() : Optional.of(host);
    }

    public int getPort() {
        return port;
    }
}
