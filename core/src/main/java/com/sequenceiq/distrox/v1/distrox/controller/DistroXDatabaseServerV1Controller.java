package com.sequenceiq.distrox.v1.distrox.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATAHUB;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXDatabaseServerV1Endpoint;

@Controller
public class DistroXDatabaseServerV1Controller implements DistroXDatabaseServerV1Endpoint {

    @Inject
    private DatabaseService databaseService;

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public StackDatabaseServerResponse getDatabaseServerByCrn(@ResourceCrn String clusterCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return databaseService.getDatabaseServer(NameOrCrn.ofCrn(clusterCrn), accountId);
    }
}
