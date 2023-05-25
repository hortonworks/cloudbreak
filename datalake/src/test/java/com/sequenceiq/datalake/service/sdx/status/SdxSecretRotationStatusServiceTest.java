package com.sequenceiq.datalake.service.sdx.status;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.datalake.entity.DatalakeStatusEnum;

@ExtendWith(MockitoExtension.class)
class SdxSecretRotationStatusServiceTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String STATUS_REASON = "statusReason";

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    private SdxSecretRotationStatusService underTest;

    @Test
    void testRotationStarted() {
        underTest.rotationStarted(RESOURCE_CRN);
        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_IN_PROGRESS), isNull(), eq(RESOURCE_CRN));
    }

    @Test
    void testRotationFinished() {
        underTest.rotationFinished(RESOURCE_CRN);
        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FINISHED), isNull(), eq(RESOURCE_CRN));
    }

    @Test
    void testRotationFailed() {
        underTest.rotationFailed(RESOURCE_CRN, STATUS_REASON);
        verify(sdxStatusService, times(1))
                .setStatusForDatalakeAndNotify(eq(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FAILED), eq(STATUS_REASON), eq(RESOURCE_CRN));
    }
}