package com.sequenceiq.datalake.controller.sdx;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.sdx.api.model.DatalakeDatabaseDrStatus;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupStatusResponse;

@ExtendWith(MockitoExtension.class)
class SdxBackupControllerTest {

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxBackupRestoreService sdxBackupRestoreService;

    @InjectMocks
    private SdxBackupController sdxBackupController;

    @Test
    public void testBackupDatabaseByName() {
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getByNameInAccount(any(), anyString())).thenReturn(sdxCluster);
        String backupId = UUID.randomUUID().toString();

        when(sdxBackupRestoreService.getDatabaseBackupStatus(sdxCluster, backupId)).thenThrow(new NotFoundException("Status entry not found"));
        sdxBackupController.backupDatabaseByName(sdxCluster.getClusterName(), backupId, "");
        verify(sdxBackupRestoreService, times(1)).triggerDatabaseBackup(any(), any());

        reset(sdxBackupRestoreService);
        when(sdxBackupRestoreService.getDatabaseBackupStatus(sdxCluster, backupId))
                .thenReturn(new SdxDatabaseBackupStatusResponse(DatalakeDatabaseDrStatus.SUCCEEDED, null));
        sdxBackupController.backupDatabaseByName(sdxCluster.getClusterName(), backupId, "");
        verify(sdxBackupRestoreService, times(0)).triggerDatabaseBackup(any(), any());
    }

    private SdxCluster getValidSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        return sdxCluster;
    }
}
