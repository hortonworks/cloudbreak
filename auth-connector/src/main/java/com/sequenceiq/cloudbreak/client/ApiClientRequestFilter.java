package com.sequenceiq.cloudbreak.client;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.ACTOR_CRN_HEADER;
import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.REQUEST_ID_HEADER;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Component
public class ApiClientRequestFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().putSingle(ACTOR_CRN_HEADER, ThreadBasedUserCrnProvider.getUserCrn());
        requestContext.getHeaders().putSingle(REQUEST_ID_HEADER, MDCBuilder.getOrGenerateRequestId());
    }
}
