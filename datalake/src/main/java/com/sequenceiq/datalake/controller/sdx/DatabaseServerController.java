package com.sequenceiq.datalake.controller.sdx;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseInstanceTypesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.common.model.DatabaseCapabilityType;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;
import com.sequenceiq.datalake.service.sdx.database.SdxDatabaseInstanceTypeService;
import com.sequenceiq.sdx.api.endpoint.DatabaseServerEndpoint;

@Controller
public class DatabaseServerController implements DatabaseServerEndpoint {

    @Inject
    private DatabaseService databaseService;

    @Inject
    private SdxDatabaseInstanceTypeService sdxDatabaseInstanceTypeService;

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATALAKE)
    public StackDatabaseServerResponse getDatabaseServerByCrn(@ResourceCrn String clusterCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return databaseService.getDatabaseServer(userCrn, clusterCrn);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_DATALAKE)
    public DatabaseInstanceTypesV4Response getDatabaseInstanceTypes(String environmentCrn, DatabaseCapabilityType databaseType, String architecture) {
        return sdxDatabaseInstanceTypeService.listDatabaseInstanceTypes(environmentCrn, databaseType, architecture);
    }
}
