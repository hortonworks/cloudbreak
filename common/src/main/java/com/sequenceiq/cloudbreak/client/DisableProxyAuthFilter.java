package com.sequenceiq.cloudbreak.client;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

public class DisableProxyAuthFilter implements ClientRequestFilter {
    @Override
    public void filter(ClientRequestContext requestContext) {
        MultivaluedMap<String, Object> headers = requestContext.getHeaders();
        headers.add("Proxy-Ignore-Auth", "true");
    }
}
