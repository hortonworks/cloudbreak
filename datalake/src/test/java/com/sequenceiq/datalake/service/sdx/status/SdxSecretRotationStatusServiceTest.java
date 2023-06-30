package com.sequenceiq.datalake.service.sdx.status;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FINISHED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_IN_PROGRESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.service.sdx.SdxService;

@ExtendWith(MockitoExtension.class)
class SdxSecretRotationStatusServiceTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String STATUS_REASON = "statusReason";

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private SdxSecretRotationStatusService underTest;

    @Test
    void testRotationStarted() {
        underTest.rotationStarted(RESOURCE_CRN);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_IN_PROGRESS), isNull(), eq(RESOURCE_CRN));
    }

    @Test
    void testRotationFinished() {
        when(sdxService.getByCrn(eq(RESOURCE_CRN))).thenReturn(new SdxCluster());
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(sdxStatusEntity(DATALAKE_SECRET_ROTATION_IN_PROGRESS));
        underTest.rotationFinished(RESOURCE_CRN);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_FINISHED), isNull(), eq(RESOURCE_CRN));
    }

    @Test
    void testRotationFinishedShouldSkipStatusUpdateWhenStatusIsAlreadySecretRotationFailed() {
        when(sdxService.getByCrn(eq(RESOURCE_CRN))).thenReturn(new SdxCluster());
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(sdxStatusEntity(DATALAKE_SECRET_ROTATION_FAILED));
        underTest.rotationFinished(RESOURCE_CRN);
        verify(sdxStatusService, never()).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_FINISHED), isNull(), eq(RESOURCE_CRN));
    }

    @Test
    void testRotationFailed() {
        underTest.rotationFailed(RESOURCE_CRN, STATUS_REASON);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_FAILED), eq(STATUS_REASON), eq(RESOURCE_CRN));
    }

    private SdxStatusEntity sdxStatusEntity(DatalakeStatusEnum datalakeStatusEnum) {
        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(datalakeStatusEnum);
        return sdxStatusEntity;
    }
}