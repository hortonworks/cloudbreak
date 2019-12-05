package com.sequenceiq.cloudbreak.client;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.logger.MDCContextFilter;
import com.sequenceiq.cloudbreak.logger.MDCUtils;

@Component
public class ApiClientRequestFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().putSingle(AbstractUserCrnServiceEndpoint.CRN_HEADER, ThreadBasedUserCrnProvider.getUserCrn());
        if (MDCUtils.getRequestId().isPresent()) {
            requestContext.getHeaders().putSingle(MDCContextFilter.REQUEST_ID_HEADER, MDCUtils.getRequestId().get());
        }
    }
}
