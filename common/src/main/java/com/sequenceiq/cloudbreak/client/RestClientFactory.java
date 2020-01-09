package com.sequenceiq.cloudbreak.client;

import javax.ws.rs.client.Client;

import org.springframework.stereotype.Component;

@Component
public class RestClientFactory {

    public Client getOrCreateDefault() {
        return RestClientUtil.get();
    }
}
