package com.sequenceiq.datalake.controller.sdx;

import java.util.Collections;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.sdx.api.endpoint.SdxBackupEndpoint;
import com.sequenceiq.sdx.api.model.SdxBackupResponse;
import com.sequenceiq.sdx.api.model.SdxBackupStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupStatusResponse;

@Controller
public class SdxBackupController implements SdxBackupEndpoint {

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.BACKUP_DATALAKE)
    public SdxDatabaseBackupResponse backupDatabaseByName(@ResourceName String name, String backupId, String backupLocation) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        try {
            SdxDatabaseBackupStatusResponse response = sdxBackupRestoreService.getDatabaseBackupStatus(sdxCluster, backupId);
            SdxDatabaseBackupResponse sdxDatabaseBackupResponse = new SdxDatabaseBackupResponse();
            sdxDatabaseBackupResponse.setOperationId(backupId);
            return sdxDatabaseBackupResponse;
        } catch (NotFoundException notFoundException) {
            SdxDatabaseBackupRequest backupRequest = new SdxDatabaseBackupRequest();
            backupRequest.setBackupId(backupId);
            backupRequest.setBackupLocation(backupLocation);
            backupRequest.setCloseConnections(true);
            backupRequest.setSkipDatabaseNames(Collections.emptyList());
            return sdxBackupRestoreService.triggerDatabaseBackup(sdxCluster, backupRequest);
        }
    }

    @Override
    @CheckPermissionByResourceName(action  = AuthorizationResourceAction.BACKUP_DATALAKE)
    public SdxDatabaseBackupResponse backupDatabaseByNameInternal(@ResourceName String name, SdxDatabaseBackupRequest backupRequest) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        String backupId = backupRequest.getBackupId();
        try {
            SdxDatabaseBackupStatusResponse response = sdxBackupRestoreService.getDatabaseBackupStatus(sdxCluster, backupId);
            SdxDatabaseBackupResponse sdxDatabaseBackupResponse = new SdxDatabaseBackupResponse();
            sdxDatabaseBackupResponse.setOperationId(backupId);
            return sdxDatabaseBackupResponse;
        } catch (NotFoundException notFoundException) {
            return sdxBackupRestoreService.triggerDatabaseBackup(sdxCluster, backupRequest);
        }
    }

    @SuppressWarnings("ParameterNumber")
    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.BACKUP_DATALAKE)
    public SdxBackupResponse backupDatalakeByName(@ResourceName String name, String backupLocation,
            String backupName, boolean skipValidation, boolean skipAtlasMetadata, boolean skipRangerAudits, boolean skipRangerMetadata,
            int fullDrMaxDurationInMin) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        return sdxBackupRestoreService.triggerDatalakeBackup(sdxCluster, backupLocation, backupName,
                new DatalakeDrSkipOptions(skipValidation, skipAtlasMetadata, skipRangerAudits, skipRangerMetadata), fullDrMaxDurationInMin);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.BACKUP_DATALAKE)
    public SdxBackupStatusResponse backupDatalakeStatusByName(@ResourceName String name,
            String backupId,
            String backupName) {
        return sdxBackupRestoreService.getDatalakeBackupStatus(name, backupId, backupName,
                ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.BACKUP_DATALAKE)
    public SdxBackupStatusResponse getBackupDatalakeStatus(@ResourceName String name,
            String backupId,
            String backupName) {
        return sdxBackupRestoreService.getDatalakeBackupStatus(name, backupId, backupName,
                ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.BACKUP_DATALAKE)
    public String getDatalakeBackupId(@ResourceName String name, String backupName) {
        return sdxBackupRestoreService.getDatalakeBackupId(name, backupName, ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.BACKUP_DATALAKE)
    public SdxDatabaseBackupStatusResponse getBackupDatabaseStatusByName(@ResourceName String name, String operationId) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        return sdxBackupRestoreService.getDatabaseBackupStatus(sdxCluster, operationId);
    }

    private SdxCluster getSdxClusterByName(String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByNameInAccount(userCrn, name);
        MDCBuilder.buildMdcContext(sdxCluster);
        return sdxCluster;
    }
}
