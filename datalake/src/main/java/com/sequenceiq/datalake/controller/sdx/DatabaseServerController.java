package com.sequenceiq.datalake.controller.sdx;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseServerChangeInstanceTypeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseServerIncreaseStorageSizeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseInstanceTypesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerChangeInstanceTypeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerIncreaseStorageSizeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerValidInstanceTypeOptionsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.common.model.DatabaseCapabilityType;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;
import com.sequenceiq.datalake.service.sdx.database.SdxDatabaseInstanceTypeService;
import com.sequenceiq.datalake.service.sdx.database.SdxDatabaseServerChangeInstanceTypeService;
import com.sequenceiq.datalake.service.sdx.database.SdxDatabaseServerIncreaseStorageSizeService;
import com.sequenceiq.sdx.api.endpoint.DatabaseServerEndpoint;

@Controller
public class DatabaseServerController implements DatabaseServerEndpoint {

    @Inject
    private DatabaseService databaseService;

    @Inject
    private SdxDatabaseInstanceTypeService sdxDatabaseInstanceTypeService;

    @Inject
    private SdxDatabaseServerChangeInstanceTypeService sdxDatabaseServerChangeInstanceTypeService;

    @Inject
    private SdxDatabaseServerIncreaseStorageSizeService sdxDatabaseServerIncreaseStorageSizeService;

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

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DATALAKE_VERTICAL_SCALING)
    public DatabaseServerChangeInstanceTypeV4Response changeDatabaseServerInstanceTypeByName(@ResourceName String name,
            DatabaseServerChangeInstanceTypeV4Request request) {
        return sdxDatabaseServerChangeInstanceTypeService.changeInstanceType(ThreadBasedUserCrnProvider.getUserCrn(), NameOrCrn.ofName(name),
                request.getInstanceType());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DATALAKE_VERTICAL_SCALING)
    public DatabaseServerChangeInstanceTypeV4Response changeDatabaseServerInstanceTypeByCrn(@ResourceCrn String crn,
            DatabaseServerChangeInstanceTypeV4Request request) {
        return sdxDatabaseServerChangeInstanceTypeService.changeInstanceType(ThreadBasedUserCrnProvider.getUserCrn(), NameOrCrn.ofCrn(crn),
                request.getInstanceType());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DATALAKE_VERTICAL_SCALING)
    public DatabaseServerIncreaseStorageSizeV4Response increaseDatabaseServerStorageSizeByName(@ResourceName String name,
            DatabaseServerIncreaseStorageSizeV4Request request) {
        return sdxDatabaseServerIncreaseStorageSizeService.increaseStorageSize(ThreadBasedUserCrnProvider.getUserCrn(), NameOrCrn.ofName(name),
                request.getStorageSizeInGb());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DATALAKE_VERTICAL_SCALING)
    public DatabaseServerIncreaseStorageSizeV4Response increaseDatabaseServerStorageSizeByCrn(@ResourceCrn String crn,
            DatabaseServerIncreaseStorageSizeV4Request request) {
        return sdxDatabaseServerIncreaseStorageSizeService.increaseStorageSize(ThreadBasedUserCrnProvider.getUserCrn(), NameOrCrn.ofCrn(crn),
                request.getStorageSizeInGb());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_DATALAKE)
    public DatabaseServerValidInstanceTypeOptionsV4Response getDatabaseServerValidInstanceTypeOptionsByName(@ResourceName String name) {
        return sdxDatabaseServerChangeInstanceTypeService.getValidInstanceTypeOptions(ThreadBasedUserCrnProvider.getUserCrn(), NameOrCrn.ofName(name));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATALAKE)
    public DatabaseServerValidInstanceTypeOptionsV4Response getDatabaseServerValidInstanceTypeOptionsByCrn(@ResourceCrn String crn) {
        return sdxDatabaseServerChangeInstanceTypeService.getValidInstanceTypeOptions(ThreadBasedUserCrnProvider.getUserCrn(), NameOrCrn.ofCrn(crn));
    }
}
