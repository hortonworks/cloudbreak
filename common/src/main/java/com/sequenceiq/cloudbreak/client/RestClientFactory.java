package com.sequenceiq.cloudbreak.client;

import java.net.URI;
import java.util.Locale;

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

    public Client getOrCreateWithFollowRedirectsAndSecureForS3(String url) {
        String host = URI.create(url).getHost();
        ConfigKey configKey = ConfigKey.builder()
                .withFollowRedirects(true)
                .withSecure(host.toLowerCase(Locale.ROOT).endsWith("s3.amazonaws.com"))
                .build();
        return getOrCreate(configKey);
    }

    public Client getOrCreateWithoutFollowRedirects() {
        return getOrCreate(ConfigKey.builder().withFollowRedirects(false).build());
    }
}
