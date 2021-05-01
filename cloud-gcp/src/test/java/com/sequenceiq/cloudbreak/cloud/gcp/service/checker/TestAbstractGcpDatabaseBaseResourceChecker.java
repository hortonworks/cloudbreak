package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public class TestAbstractGcpDatabaseBaseResourceChecker extends AbstractGcpDatabaseBaseResourceChecker {

    public CloudResource createOperationAwareCloudResource(CloudResource resource, com.google.api.services.sqladmin.model.Operation operation) {
        return super.createOperationAwareCloudResource(resource, operation);
    }
}
