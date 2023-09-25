package com.sequenceiq.cloudbreak.rotation.secret.custom;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.RotationMetadataTestUtil;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;

@ExtendWith(MockitoExtension.class)
class CustomJobRotationExecutorTest {

    @Mock
    private Runnable rotateCustomJob;

    @Mock
    private Runnable rollbackCustomJob;

    @Mock
    private Runnable finalizeCustomJob;

    @Mock
    private Runnable preValidateCustomJob;

    @Mock
    private Runnable postValidateCustomJob;

    @Mock
    private SecretRotationStepProgressService secretRotationProgressService;

    @Mock
    private SecretRotationNotificationService secretRotationNotificationService;

    @InjectMocks
    private CustomJobRotationExecutor underTest;

    @BeforeEach
    public void mockProgressService() {
        lenient().when(secretRotationProgressService.latestStep(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    public void testRotation() throws Exception {
        underTest.executeRotate(createContext(rotateCustomJob, null, null, null, null),
                RotationMetadataTestUtil.metadataForRotation("resource", null));

        verify(rotateCustomJob).run();
        verify(rollbackCustomJob, times(0)).run();
        verify(finalizeCustomJob, times(0)).run();
        verify(preValidateCustomJob, times(0)).run();
        verify(postValidateCustomJob, times(0)).run();
    }

    @Test
    public void testRotationFailure() {
        doThrow(new RuntimeException("something")).when(rotateCustomJob).run();

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRotate(createContext(rotateCustomJob, null, null, null, null),
                        RotationMetadataTestUtil.metadataForRotation("resource", null)));

        verify(rotateCustomJob).run();
        verify(rollbackCustomJob, times(0)).run();
        verify(finalizeCustomJob, times(0)).run();
        verify(preValidateCustomJob, times(0)).run();
        verify(postValidateCustomJob, times(0)).run();
    }

    @Test
    public void testRollbackFailure() {
        doThrow(new RuntimeException("something")).when(rollbackCustomJob).run();

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRollback(createContext(rotateCustomJob, rollbackCustomJob, null, null, null),
                        RotationMetadataTestUtil.metadataForRollback("resource", null)));

        verify(rollbackCustomJob).run();
        verify(rotateCustomJob, times(0)).run();
        verify(finalizeCustomJob, times(0)).run();
        verify(preValidateCustomJob, times(0)).run();
        verify(postValidateCustomJob, times(0)).run();
    }

    @Test
    public void testFinalization() throws Exception {
        underTest.executeFinalize(createContext(rotateCustomJob, null, finalizeCustomJob, null, null),
                RotationMetadataTestUtil.metadataForFinalize("resource", null));

        verify(finalizeCustomJob).run();
        verify(rotateCustomJob, times(0)).run();
        verify(rollbackCustomJob, times(0)).run();
        verify(preValidateCustomJob, times(0)).run();
        verify(postValidateCustomJob, times(0)).run();
    }

    @Test
    public void testFinalizationFailure() {
        doThrow(new RuntimeException("something")).when(finalizeCustomJob).run();

        assertThrows(SecretRotationException.class, () ->
                underTest.executeFinalize(createContext(rotateCustomJob, null, finalizeCustomJob, null, null),
                        RotationMetadataTestUtil.metadataForFinalize("resource", null)));

        verify(finalizeCustomJob).run();
        verify(rotateCustomJob, times(0)).run();
        verify(rollbackCustomJob, times(0)).run();
        verify(preValidateCustomJob, times(0)).run();
        verify(postValidateCustomJob, times(0)).run();
    }

    @Test
    public void testPreValidate() throws Exception {
        underTest.executePreValidation(createContext(rotateCustomJob, null, null, preValidateCustomJob, null),
                RotationMetadataTestUtil.metadataForPreValidate("resource", null));

        verify(preValidateCustomJob).run();
        verify(rotateCustomJob, times(0)).run();
        verify(rollbackCustomJob, times(0)).run();
        verify(finalizeCustomJob, times(0)).run();
        verify(postValidateCustomJob, times(0)).run();
    }

    @Test
    public void testPreValidateFailure() {
        doThrow(new RuntimeException("something")).when(preValidateCustomJob).run();

        assertThrows(SecretRotationException.class, () ->
                underTest.executePreValidation(createContext(rotateCustomJob, null, null, preValidateCustomJob, null),
                        RotationMetadataTestUtil.metadataForPreValidate("resource", null)));

        verify(preValidateCustomJob).run();
        verify(rotateCustomJob, times(0)).run();
        verify(rollbackCustomJob, times(0)).run();
        verify(finalizeCustomJob, times(0)).run();
        verify(postValidateCustomJob, times(0)).run();
    }

    @Test
    public void testPostValidate() throws Exception {
        underTest.executePostValidation(createContext(rotateCustomJob, null, null, null, postValidateCustomJob));

        verify(postValidateCustomJob).run();
        verify(rotateCustomJob, times(0)).run();
        verify(rollbackCustomJob, times(0)).run();
        verify(preValidateCustomJob, times(0)).run();
        verify(finalizeCustomJob, times(0)).run();
    }

    @Test
    public void testPostValidateFailure() {
        doThrow(new RuntimeException("something")).when(postValidateCustomJob).run();

        assertThrows(SecretRotationException.class, () ->
                underTest.executePostValidation(createContext(rotateCustomJob, null, null, null, postValidateCustomJob)));

        verify(postValidateCustomJob).run();
        verify(rotateCustomJob, times(0)).run();
        verify(rollbackCustomJob, times(0)).run();
        verify(preValidateCustomJob, times(0)).run();
        verify(finalizeCustomJob, times(0)).run();
    }

    private RotationContext createContext(Runnable rotateCustomJob, Runnable rollbackCustomJob, Runnable finalizeCustomJob,
            Runnable preValidateCustomJob, Runnable postValidateCustomJob) {
        return CustomJobRotationContext.builder()
                .withRotationJob(rotateCustomJob)
                .withRollbackJob(rollbackCustomJob)
                .withFinalizeJob(finalizeCustomJob)
                .withPreValidateJob(preValidateCustomJob)
                .withPostValidateJob(postValidateCustomJob)
                .build();
    }
}