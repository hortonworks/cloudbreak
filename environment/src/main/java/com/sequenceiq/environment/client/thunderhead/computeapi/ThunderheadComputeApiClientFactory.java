package com.sequenceiq.environment.client.thunderhead.computeapi;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.cloudera.thunderheadcompute.api.DefaultApi;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MDCContextFilter;
import com.sequenceiq.thunderheadcompute.ApiClient;

@Service
public class ThunderheadComputeApiClientFactory {

    private static final String X_CDP_ACTOR_CRN = "x-cdp-actor-crn";

    @Inject
    private ThunderheadComputeApiClientConfig thunderheadComputeApiClientConfig;

    public DefaultApi create() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(thunderheadComputeApiClientConfig.getClientConnectionUrl());
        apiClient.setDebugging(Boolean.TRUE);
        apiClient.addDefaultHeader(X_CDP_ACTOR_CRN, ThreadBasedUserCrnProvider.getUserCrn());
        apiClient.addDefaultHeader(MDCContextFilter.REQUEST_ID_HEADER, MDCBuilder.getOrGenerateRequestId());
        return new DefaultApi(apiClient);
    }

}
