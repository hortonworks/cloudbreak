package com.sequenceiq.cloudbreak.client;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MDCRequestIdOnlyFilter;

@Component
public class ApiClientRequestFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().putSingle(AbstractUserCrnServiceEndpoint.CRN_HEADER, ThreadBasedUserCrnProvider.getUserCrn());
        requestContext.getHeaders().putSingle(MDCRequestIdOnlyFilter.REQUEST_ID_HEADER, MDCBuilder.getOrGenerateRequestId());
    }
}
