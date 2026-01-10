package com.sequenceiq.cloudbreak.client;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.REQUEST_ID_HEADER;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public class RequestIdProviderFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().putSingle(REQUEST_ID_HEADER, MDCBuilder.getOrGenerateRequestId());
    }
}
