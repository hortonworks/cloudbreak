package com.sequenceiq.cloudbreak.client;

import jakarta.ws.rs.client.Client;

import org.springframework.stereotype.Component;

@Component
public class RestClientFactory {

    public Client getOrCreateDefault() {
        return RestClientUtil.get();
    }

    public Client getOrCreate(ConfigKey configKey) {
        return RestClientUtil.get(configKey);
    }

    public Client getOrCreateWithFollowRedirects() {
        return getOrCreate(ConfigKey.builder().withFollowRedirects(true).build());
    }
}
