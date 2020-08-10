package com.sequenceiq.cloudbreak.client;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCContextFilter;
import com.sequenceiq.cloudbreak.logger.MDCUtils;

@Component
public class ApiInternalClientRequestFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().putSingle(AbstractUserCrnServiceEndpoint.CRN_HEADER, INTERNAL_ACTOR_CRN);
        if (MDCUtils.getRequestId().isPresent()) {
            requestContext.getHeaders().putSingle(MDCContextFilter.REQUEST_ID_HEADER, MDCUtils.getRequestId().get());
        }
    }
}
