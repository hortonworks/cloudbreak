package com.sequenceiq.externalizedcompute.api.client;

import jakarta.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.AbstractKeyBasedServiceEndpoint;
import com.sequenceiq.externalizedcompute.api.endpoint.ExternalizedComputeClusterEndpoint;
import com.sequenceiq.externalizedcompute.api.endpoint.ExternalizedComputeClusterInternalEndpoint;
import com.sequenceiq.flow.api.FlowPublicEndpoint;

public class ExternalizedComputeClusterApiKeyEndpoints extends AbstractKeyBasedServiceEndpoint implements ExternalizedComputeClusterClient {

    ExternalizedComputeClusterApiKeyEndpoints(WebTarget webTarget, String accessKey, String secretKey) {
        super(webTarget, accessKey, secretKey);
    }

    @Override
    public ExternalizedComputeClusterEndpoint externalizedComputeClusterEndpoint() {
        return getEndpoint(ExternalizedComputeClusterEndpoint.class);
    }

    @Override
    public ExternalizedComputeClusterInternalEndpoint externalizedComputeClusterInternalEndpoint() {
        return getEndpoint(ExternalizedComputeClusterInternalEndpoint.class);
    }

    @Override
    public FlowPublicEndpoint getFlowPublicEndpoint() {
        return getEndpoint(FlowPublicEndpoint.class);
    }
}
