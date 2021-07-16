package com.sequenceiq.datalake.controller.sdx;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.sdx.api.endpoint.SdxRestoreEndpoint;
import com.sequenceiq.sdx.api.model.SdxDatabaseRestoreResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRestoreStatusResponse;
import com.sequenceiq.sdx.api.model.SdxRestoreResponse;
import com.sequenceiq.sdx.api.model.SdxRestoreStatusResponse;

@Controller
public class SdxRestoreController implements SdxRestoreEndpoint {

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RESTORE_DATALAKE)
    public SdxDatabaseRestoreResponse restoreDatabaseByName(@ResourceName String name, String backupId,
            String restoreId, String backupLocation) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        try {
            sdxBackupRestoreService.getDatabaseRestoreStatus(sdxCluster, restoreId);
            SdxDatabaseRestoreResponse sdxDatabaseRestoreResponse = new SdxDatabaseRestoreResponse();
            sdxDatabaseRestoreResponse.setOperationId(restoreId);
            return sdxDatabaseRestoreResponse;
        } catch (NotFoundException notFoundException) {
            return sdxBackupRestoreService.triggerDatabaseRestore(sdxCluster, backupId, restoreId, backupLocation);
        }
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RESTORE_DATALAKE)
    public SdxRestoreResponse restoreDatalakeByName(@ResourceName String name, String backupId, String backupLocationOverride) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        return sdxBackupRestoreService.triggerDatalakeRestore(sdxCluster, name, backupId, backupLocationOverride);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RESTORE_DATALAKE)
    public SdxRestoreStatusResponse getRestoreDatalakeStatusByName(@ResourceName String name,
                                                                String restoreId) {
        return sdxBackupRestoreService.getDatalakeRestoreStatus(name, restoreId, null, ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RESTORE_DATALAKE)
    public SdxRestoreStatusResponse getRestoreDatalakeStatus(@ResourceName String name,
            String restoreId, String backupName) {
        return sdxBackupRestoreService.getDatalakeRestoreStatus(name, restoreId, backupName, ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RESTORE_DATALAKE)
    public String getDatalakeRestoreId(@ResourceName String name, String backupName) {
        return sdxBackupRestoreService.getDatalakeRestoreId(name, backupName, ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RESTORE_DATALAKE)
    public SdxDatabaseRestoreStatusResponse getRestoreDatabaseStatusByName(@ResourceName String name, String operationId) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        return sdxBackupRestoreService.getDatabaseRestoreStatus(sdxCluster, operationId);
    }

    private SdxCluster getSdxClusterByName(String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByNameInAccount(userCrn, name);
        MDCBuilder.buildMdcContext(sdxCluster);
        return sdxCluster;
    }
}
