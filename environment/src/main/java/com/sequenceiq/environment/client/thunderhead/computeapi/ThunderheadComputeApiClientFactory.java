package com.sequenceiq.environment.client.thunderhead.computeapi;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.ACTOR_CRN_HEADER;
import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.REQUEST_ID_HEADER;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.cloudera.thunderheadcompute.api.DefaultApi;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.thunderheadcompute.ApiClient;

@Service
public class ThunderheadComputeApiClientFactory {

    @Inject
    private ThunderheadComputeApiClientConfig thunderheadComputeApiClientConfig;

    public DefaultApi create() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(thunderheadComputeApiClientConfig.getClientConnectionUrl());
        apiClient.setDebugging(Boolean.TRUE);
        apiClient.addDefaultHeader(ACTOR_CRN_HEADER, ThreadBasedUserCrnProvider.getUserCrn());
        apiClient.addDefaultHeader(REQUEST_ID_HEADER, MDCBuilder.getOrGenerateRequestId());
        return new DefaultApi(apiClient);
    }

}
