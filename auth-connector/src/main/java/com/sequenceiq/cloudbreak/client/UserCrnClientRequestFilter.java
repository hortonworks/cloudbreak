package com.sequenceiq.cloudbreak.client;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

@Component
public class UserCrnClientRequestFilter implements ClientRequestFilter {
    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().putSingle(AbstractUserCrnServiceEndpoint.CRN_HEADER, threadBasedUserCrnProvider.getUserCrn());
    }
}
