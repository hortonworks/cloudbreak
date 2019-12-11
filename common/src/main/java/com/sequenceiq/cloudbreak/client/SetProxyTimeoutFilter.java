package com.sequenceiq.cloudbreak.client;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

public class SetProxyTimeoutFilter implements ClientRequestFilter {

    private final Integer timeout;

    public SetProxyTimeoutFilter(Integer timeout) {
        this.timeout = timeout;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        MultivaluedMap<String, Object> headers = requestContext.getHeaders();
        headers.add("Proxy-With-Timeout", timeout.toString());
    }
}
