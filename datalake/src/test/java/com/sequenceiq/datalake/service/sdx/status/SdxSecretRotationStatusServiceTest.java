package com.sequenceiq.datalake.service.sdx.status;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FINALIZE_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FINALIZE_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FINISHED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_ROLLBACK_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_ROLLBACK_FINISHED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_ROLLBACK_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RUNNING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@ExtendWith(MockitoExtension.class)
class SdxSecretRotationStatusServiceTest {

    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:acc1:datalake:cluster1";

    private static final SecretType SECRET_TYPE = DatalakeSecretType.DEMO_SECRET;

    private static final String REASON = "reason";

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private SdxService sdxService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @InjectMocks
    private SdxSecretRotationStatusService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(cloudbreakMessagesService.getMessage(anyString())).thenReturn(SECRET_TYPE.toString());
    }

    @Test
    void rotationStartedShouldSucceed() {
        underTest.rotationStarted(RESOURCE_CRN, SECRET_TYPE);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_IN_PROGRESS), contains(SECRET_TYPE.value()),
                anyString(), eq(RESOURCE_CRN));
    }

    @Test
    void rotationFinishedShouldSucceed() {
        underTest.rotationFinished(RESOURCE_CRN, SECRET_TYPE);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_FINISHED), contains(SECRET_TYPE.value()),
                anyString(), eq(RESOURCE_CRN));
    }

    @Test
    void rotationFailedShouldSucceed() {
        when(sdxService.getByCrn(eq(RESOURCE_CRN))).thenReturn(new SdxCluster());
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(sdxStatusEntity(DATALAKE_SECRET_ROTATION_IN_PROGRESS));
        underTest.rotationFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_FAILED), contains(SECRET_TYPE.value()),
                contains(REASON), eq(RESOURCE_CRN));
    }

    @Test
    void rotationFailedShouldSkipStatusUpdateWhenStatusIsAlreadySecretRotationRollbackFailed() {
        when(sdxService.getByCrn(eq(RESOURCE_CRN))).thenReturn(new SdxCluster());
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(sdxStatusEntity(DATALAKE_SECRET_ROTATION_ROLLBACK_FAILED));
        underTest.rotationFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(sdxStatusService, never()).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_FAILED), contains(SECRET_TYPE.value()),
                contains(REASON), eq(RESOURCE_CRN));
    }

    @Test
    void rotationFailedShouldSkipStatusUpdateWhenStatusIsAlreadySecretRotationFinalizeFailed() {
        when(sdxService.getByCrn(eq(RESOURCE_CRN))).thenReturn(new SdxCluster());
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(sdxStatusEntity(DATALAKE_SECRET_ROTATION_FINALIZE_FAILED));
        underTest.rotationFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(sdxStatusService, never()).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_FAILED), contains(SECRET_TYPE.value()),
                contains(REASON), eq(RESOURCE_CRN));
    }

    @Test
    void rollbackStartedShouldSucceed() {
        underTest.rollbackStarted(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_ROLLBACK_IN_PROGRESS),
                contains(SECRET_TYPE.value()), contains(REASON), eq(RESOURCE_CRN));
    }

    @Test
    void rollbackFinishedShouldSucceed() {
        underTest.rollbackFinished(RESOURCE_CRN, SECRET_TYPE);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_ROLLBACK_FINISHED),
                contains(SECRET_TYPE.value()), anyString(), eq(RESOURCE_CRN));
    }

    @Test
    void rollbackFailedShouldSucceed() {
        underTest.rollbackFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_ROLLBACK_FAILED),
                contains(SECRET_TYPE.value()), contains(REASON), eq(RESOURCE_CRN));
    }

    @Test
    void finalizeStartedShouldSucceed() {
        underTest.finalizeStarted(RESOURCE_CRN, SECRET_TYPE);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_FINALIZE_IN_PROGRESS),
                contains(SECRET_TYPE.value()), anyString(), eq(RESOURCE_CRN));
    }

    @Test
    void finalizeFinishedShouldSucceed() {
        underTest.finalizeFinished(RESOURCE_CRN, SECRET_TYPE);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(RUNNING), contains(SECRET_TYPE.value()),
                anyString(), eq(RESOURCE_CRN));
    }

    @Test
    void finalizeFailedShouldSucceed() {
        underTest.finalizeFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_FINALIZE_FAILED),
                contains(SECRET_TYPE.value()), contains(REASON), eq(RESOURCE_CRN));
    }

    private SdxStatusEntity sdxStatusEntity(DatalakeStatusEnum datalakeStatusEnum) {
        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(datalakeStatusEnum);
        return sdxStatusEntity;
    }
}