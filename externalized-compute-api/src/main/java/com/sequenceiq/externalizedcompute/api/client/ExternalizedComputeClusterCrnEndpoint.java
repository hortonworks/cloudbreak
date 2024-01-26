package com.sequenceiq.externalizedcompute.api.client;

import jakarta.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint;
import com.sequenceiq.externalizedcompute.api.endpoint.ExternalizedComputeClusterEndpoint;

public class ExternalizedComputeClusterCrnEndpoint extends AbstractUserCrnServiceEndpoint implements ExternalizedComputeClusterClient {

    public ExternalizedComputeClusterCrnEndpoint(WebTarget webTarget, String crn) {
        super(webTarget, crn);
    }

    @Override
    public ExternalizedComputeClusterEndpoint externalizedComputeClusterEndpoint() {
        return getEndpoint(ExternalizedComputeClusterEndpoint.class);
    }
}
