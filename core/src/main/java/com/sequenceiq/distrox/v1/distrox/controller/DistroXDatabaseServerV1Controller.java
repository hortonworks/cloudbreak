package com.sequenceiq.distrox.v1.distrox.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DATAHUB_VERTICAL_SCALING;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATAHUB;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.ENVIRONMENT_CREATE_DATAHUB;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceName;
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
import com.sequenceiq.cloudbreak.service.database.DatabaseInstanceTypeService;
import com.sequenceiq.cloudbreak.service.database.DatabaseServerChangeInstanceTypeService;
import com.sequenceiq.cloudbreak.service.database.DatabaseServerIncreaseStorageSizeService;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.common.model.DatabaseCapabilityType;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXDatabaseServerV1Endpoint;

@Controller
public class DistroXDatabaseServerV1Controller implements DistroXDatabaseServerV1Endpoint {

    @Inject
    private DatabaseService databaseService;

    @Inject
    private DatabaseInstanceTypeService databaseInstanceTypeService;

    @Inject
    private DatabaseServerChangeInstanceTypeService databaseServerChangeInstanceTypeService;

    @Inject
    private DatabaseServerIncreaseStorageSizeService databaseServerIncreaseStorageSizeService;

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public StackDatabaseServerResponse getDatabaseServerByCrn(@ResourceCrn String clusterCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return databaseService.getDatabaseServer(NameOrCrn.ofCrn(clusterCrn), accountId);
    }

    @Override
    @CheckPermissionByAccount(action = ENVIRONMENT_CREATE_DATAHUB)
    public DatabaseInstanceTypesV4Response getDatabaseInstanceTypes(String environmentCrn, DatabaseCapabilityType databaseType, String architecture) {
        return databaseInstanceTypeService.listDatabaseInstanceTypes(environmentCrn, databaseType, architecture);
    }

    @Override
    @CheckPermissionByResourceName(action = DATAHUB_VERTICAL_SCALING)
    public DatabaseServerChangeInstanceTypeV4Response changeDatabaseServerInstanceTypeByName(@ResourceName String name,
            DatabaseServerChangeInstanceTypeV4Request request) {
        return databaseServerChangeInstanceTypeService.changeInstanceType(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(),
                request.getInstanceType());
    }

    @Override
    @CheckPermissionByResourceCrn(action = DATAHUB_VERTICAL_SCALING)
    public DatabaseServerChangeInstanceTypeV4Response changeDatabaseServerInstanceTypeByCrn(@ResourceCrn String crn,
            DatabaseServerChangeInstanceTypeV4Request request) {
        return databaseServerChangeInstanceTypeService.changeInstanceType(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId(),
                request.getInstanceType());
    }

    @Override
    @CheckPermissionByResourceName(action = DATAHUB_VERTICAL_SCALING)
    public DatabaseServerIncreaseStorageSizeV4Response increaseDatabaseServerStorageSizeByName(@ResourceName String name,
            DatabaseServerIncreaseStorageSizeV4Request request) {
        return databaseServerIncreaseStorageSizeService.increaseStorageSize(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(),
                request.getStorageSizeInGb());
    }

    @Override
    @CheckPermissionByResourceCrn(action = DATAHUB_VERTICAL_SCALING)
    public DatabaseServerIncreaseStorageSizeV4Response increaseDatabaseServerStorageSizeByCrn(@ResourceCrn String crn,
            DatabaseServerIncreaseStorageSizeV4Request request) {
        return databaseServerIncreaseStorageSizeService.increaseStorageSize(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId(),
                request.getStorageSizeInGb());
    }

    @Override
    @CheckPermissionByResourceName(action = DESCRIBE_DATAHUB)
    public DatabaseServerValidInstanceTypeOptionsV4Response getDatabaseServerValidInstanceTypeOptionsByName(@ResourceName String name) {
        return databaseServerChangeInstanceTypeService.getValidInstanceTypeOptions(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public DatabaseServerValidInstanceTypeOptionsV4Response getDatabaseServerValidInstanceTypeOptionsByCrn(@ResourceCrn String crn) {
        return databaseServerChangeInstanceTypeService.getValidInstanceTypeOptions(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId());
    }
}
