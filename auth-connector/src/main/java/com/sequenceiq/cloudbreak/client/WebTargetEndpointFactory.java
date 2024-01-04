package com.sequenceiq.cloudbreak.client;

import jakarta.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.proxy.WebResourceFactory;

public class WebTargetEndpointFactory {
    public <T> T createEndpoint(WebTarget webTarget, Class<T> endpointClass) {
        return WebResourceFactory.newResource(endpointClass, webTarget);
    }
}
