package com.sequenceiq.cloudbreak.client;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBaseUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.token.CrnTokenExtractor;

@Component
public class UserCrnClientRequestFilter implements ClientRequestFilter {
    @Inject
    private ThreadBaseUserCrnProvider threadBaseUserCrnProvider;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().putSingle(CrnTokenExtractor.CRN_HEADER, threadBaseUserCrnProvider.getUserCrn());
    }
}
