package com.sequenceiq.cloudbreak.cloud.gcp;

import com.sequenceiq.common.api.type.ResourceType;

public class TestAbstractGcpResourceBuilder extends AbstractGcpResourceBuilder {

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_BACKEND_SERVICE;
    }
}
