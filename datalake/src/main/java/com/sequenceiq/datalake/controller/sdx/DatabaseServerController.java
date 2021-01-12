package com.sequenceiq.datalake.controller.sdx;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.sdx.api.endpoint.DatabaseServerEndpoint;

@Controller
public class DatabaseServerController implements DatabaseServerEndpoint {

    @Inject
    private DatabaseService databaseService;

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATALAKE)
    public StackDatabaseServerResponse getDatabaseServerByCrn(@TenantAwareParam @ResourceCrn String clusterCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return databaseService.getDatabaseServer(userCrn, clusterCrn);
    }
}
