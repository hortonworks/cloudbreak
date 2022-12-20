package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import java.util.Collections;

import com.google.api.services.sqladmin.model.Operation;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;

public abstract class AbstractGcpDatabaseBaseResourceChecker extends AbstractGcpBaseResourceChecker {

    public static final String OPERATION_ID = "opid";

    protected CloudResource createOperationAwareCloudResource(CloudResource resource, Operation operation) {
        return new Builder()
                .cloudResource(resource)
                .withParameters(Collections.singletonMap(OPERATION_ID, operation.getName()))
                .withPersistent(false)
                .build();
    }
}
