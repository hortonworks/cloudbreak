package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import java.util.Collections;

import com.google.api.services.sqladmin.model.Operation;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpOperationUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public abstract class AbstractGcpDatabaseBaseResourceChecker extends AbstractGcpBaseResourceChecker {
    protected CloudResource createOperationAwareCloudResource(CloudResource resource, Operation operation) {
        return CloudResource.builder()
                .cloudResource(resource)
                .withParameters(Collections.singletonMap(GcpOperationUtil.OPERATION_ID, operation.getName()))
                .withPersistent(false)
                .build();
    }
}
