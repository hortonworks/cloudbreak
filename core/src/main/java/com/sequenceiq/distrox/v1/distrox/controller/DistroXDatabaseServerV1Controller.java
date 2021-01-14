package com.sequenceiq.distrox.v1.distrox.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATAHUB;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXDatabaseServerV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;

@Controller
public class DistroXDatabaseServerV1Controller implements DistroXDatabaseServerV1Endpoint {

    @Inject
    private DatabaseService databaseService;

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public StackDatabaseServerResponse getDatabaseServerByCrn(@TenantAwareParam @ResourceCrn String clusterCrn) {
        return databaseService.getDatabaseServer(clusterCrn);
    }
}
