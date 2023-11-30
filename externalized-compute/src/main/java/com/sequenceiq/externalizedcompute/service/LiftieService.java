package com.sequenceiq.externalizedcompute.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudera.api.DefaultApi;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MDCContextFilter;
import com.sequenceiq.externalizedcompute.ApiClient;
import com.sequenceiq.externalizedcompute.liftie.client.LiftieClientFactory;
import com.sequenceiq.externalizedcompute.liftie.client.LiftieEndpoint;

@Service
public class LiftieService {

    private static final String X_CDP_ACTOR_CRN = "x-cdp-actor-crn";

    @Value("${externalizedcompute.liftie.host}")
    private String liftieHost;

    @Value("${externalizedcompute.liftie.port}")
    private int liftiePort;

    public DefaultApi getDefaultApi() {
        String requestId = MDCBuilder.getOrGenerateRequestId();
        ApiClient liftieClient = LiftieClientFactory.getApiClient(new LiftieEndpoint(liftieHost, liftiePort));
        liftieClient.setDebugging(true);
        liftieClient.addDefaultHeader(X_CDP_ACTOR_CRN, ThreadBasedUserCrnProvider.getUserCrn());
        liftieClient.addDefaultHeader(MDCContextFilter.REQUEST_ID_HEADER, requestId);
        return new DefaultApi(liftieClient);
    }

}
