package com.sequenceiq.cloudbreak.client;

import static com.sequenceiq.cloudbreak.client.RequestIdProvider.REQUEST_ID_HEADER;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

public class RequestIdProviderFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().putSingle(REQUEST_ID_HEADER, RequestIdProvider.getOrGenerateRequestId());
    }
}
