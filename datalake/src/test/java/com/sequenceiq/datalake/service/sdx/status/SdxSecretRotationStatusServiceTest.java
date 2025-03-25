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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;
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
    private SecretRotationNotificationService secretRotationNotificationService;

    @InjectMocks
    private SdxSecretRotationStatusService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(secretRotationNotificationService.getMessage(any(), any())).thenReturn(SECRET_TYPE.toString());
        when(sdxService.getByCrn(anyString())).thenReturn(new SdxCluster());
    }

    @Test
    void rotationStartedShouldSucceed() {
        underTest.rotationStarted(RESOURCE_CRN, SECRET_TYPE);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_IN_PROGRESS),
                eq(List.of(SECRET_TYPE.value())), eq("Secret rotation started: DEMO_SECRET"), any(SdxCluster.class));
    }

    @Test
    void rotationFinishedShouldSucceed() {
        underTest.rotationFinished(RESOURCE_CRN, SECRET_TYPE);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_FINISHED),
                eq(List.of(SECRET_TYPE.value())), eq("Secret rotation finished: DEMO_SECRET"), any(SdxCluster.class));
    }

    @Test
    void rotationFailedShouldSucceed() {
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(sdxStatusEntity(DATALAKE_SECRET_ROTATION_IN_PROGRESS));
        underTest.rotationFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_FAILED),
                eq(List.of(SECRET_TYPE.value(), REASON)), eq("Secret rotation failed: DEMO_SECRET"), any(SdxCluster.class));
    }

    @Test
    void rotationFailedShouldSkipStatusUpdateWhenStatusIsAlreadySecretRotationRollbackFailed() {
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(sdxStatusEntity(DATALAKE_SECRET_ROTATION_ROLLBACK_FAILED));
        underTest.rotationFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(sdxStatusService, never()).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_FAILED), anyCollection(), anyString(),
                any(SdxCluster.class));
    }

    @Test
    void rotationFailedShouldSkipStatusUpdateWhenStatusIsAlreadySecretRotationFinalizeFailed() {
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(sdxStatusEntity(DATALAKE_SECRET_ROTATION_FINALIZE_FAILED));
        underTest.rotationFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(sdxStatusService, never()).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_FAILED), anyCollection(), anyString(),
                any(SdxCluster.class));
    }

    @Test
    void rollbackStartedShouldSucceed() {
        underTest.rollbackStarted(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_ROLLBACK_IN_PROGRESS),
                eq(List.of(SECRET_TYPE.value(), REASON)), eq("Secret rotation rollback started: DEMO_SECRET"), any(SdxCluster.class));
    }

    @Test
    void rollbackFinishedShouldSucceed() {
        underTest.rollbackFinished(RESOURCE_CRN, SECRET_TYPE);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_ROLLBACK_FINISHED),
                eq(List.of(SECRET_TYPE.value())), eq("Secret rotation rollback finished: DEMO_SECRET"), any(SdxCluster.class));
    }

    @Test
    void rollbackFailedShouldSucceed() {
        underTest.rollbackFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_ROLLBACK_FAILED),
                eq(List.of(SECRET_TYPE.value(), REASON)), eq("Secret rotation rollback failed: DEMO_SECRET"), any(SdxCluster.class));
    }

    @Test
    void finalizeStartedShouldSucceed() {
        underTest.finalizeStarted(RESOURCE_CRN, SECRET_TYPE);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_FINALIZE_IN_PROGRESS),
                eq(List.of(SECRET_TYPE.value())), eq("Secret rotation finalize started: DEMO_SECRET"), any(SdxCluster.class));
    }

    @Test
    void finalizeFinishedShouldSucceed() {
        underTest.finalizeFinished(RESOURCE_CRN, SECRET_TYPE);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(RUNNING), eq(ResourceEvent.SECRET_ROTATION_FINALIZE_FINISHED),
                eq(List.of(SECRET_TYPE.value())), eq("Secret rotation finalize finished: DEMO_SECRET"), any(SdxCluster.class));
    }

    @Test
    void finalizeFailedShouldSucceed() {
        underTest.finalizeFailed(RESOURCE_CRN, SECRET_TYPE, REASON);
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(DATALAKE_SECRET_ROTATION_FINALIZE_FAILED),
                eq(List.of(SECRET_TYPE.value(), REASON)), eq("Secret rotation finalize failed: DEMO_SECRET"), any(SdxCluster.class));
    }

    private SdxStatusEntity sdxStatusEntity(DatalakeStatusEnum datalakeStatusEnum) {
        SdxStatusEntity sdxStatusEntity = new SdxStatusEntity();
        sdxStatusEntity.setStatus(datalakeStatusEnum);
        return sdxStatusEntity;
    }
}