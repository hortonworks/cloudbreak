package com.sequenceiq.externalizedcompute.api.client;

import com.sequenceiq.externalizedcompute.api.endpoint.ExternalizedComputeClusterEndpoint;
import com.sequenceiq.flow.api.FlowPublicEndpoint;

public interface ExternalizedComputeClusterClient {
    ExternalizedComputeClusterEndpoint externalizedComputeClusterEndpoint();

    FlowPublicEndpoint getFlowPublicEndpoint();
}
