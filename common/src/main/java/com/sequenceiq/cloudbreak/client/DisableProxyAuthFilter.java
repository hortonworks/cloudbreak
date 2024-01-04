package com.sequenceiq.cloudbreak.client;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;

public class DisableProxyAuthFilter implements ClientRequestFilter {
    @Override
    public void filter(ClientRequestContext requestContext) {
        MultivaluedMap<String, Object> headers = requestContext.getHeaders();
        headers.add("Proxy-Ignore-Auth", "true");
    }
}
