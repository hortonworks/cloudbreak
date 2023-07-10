package com.sequenceiq.cloudbreak.rotation.executor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationProgressService;

@ExtendWith(MockitoExtension.class)
class CustomJobExecutorTest {

    @Mock
    private Runnable rotateCustomJob;

    @Mock
    private Runnable rollbackCustomJob;

    @Mock
    private Runnable finalizeCustomJob;

    @Mock
    private SecretRotationProgressService secretRotationProgressService;

    @InjectMocks
    private CustomJobExecutor underTest;

    @BeforeEach
    public void mockProgressService() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "secretRotationProgressService", Optional.of(secretRotationProgressService), true);
        lenient().when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.empty());
    }

    @Test
    public void testRotation() throws Exception {
        underTest.executeRotate(createContext(rotateCustomJob, null, null), null);

        verify(rotateCustomJob).run();
        verify(rollbackCustomJob, times(0)).run();
        verify(finalizeCustomJob, times(0)).run();
    }

    @Test
    public void testRotationFailure() {
        doThrow(new RuntimeException("something")).when(rotateCustomJob).run();

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRotate(createContext(rotateCustomJob, null, null), null));

        verify(rotateCustomJob).run();
        verify(rollbackCustomJob, times(0)).run();
        verify(finalizeCustomJob, times(0)).run();
    }

    @Test
    public void testRollbackFailure() {
        doThrow(new RuntimeException("something")).when(rollbackCustomJob).run();

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRollback(createContext(rotateCustomJob, rollbackCustomJob, null), null));

        verify(rotateCustomJob, times(0)).run();
        verify(rollbackCustomJob).run();
        verify(finalizeCustomJob, times(0)).run();
    }

    @Test
    public void testFinalization() throws Exception {
        underTest.executeFinalize(createContext(rotateCustomJob, null, finalizeCustomJob), null);

        verify(rotateCustomJob, times(0)).run();
        verify(rollbackCustomJob, times(0)).run();
        verify(finalizeCustomJob, times(1)).run();
    }

    @Test
    public void testFinalizationFailure() {
        doThrow(new RuntimeException("something")).when(finalizeCustomJob).run();

        assertThrows(SecretRotationException.class, () ->
                underTest.executeFinalize(createContext(rotateCustomJob, null, finalizeCustomJob), null));

        verify(rotateCustomJob, times(0)).run();
        verify(rollbackCustomJob, times(0)).run();
        verify(finalizeCustomJob, times(1)).run();
    }

    private RotationContext createContext(Runnable rotateCustomJob, Runnable rollbackCustomJob, Runnable finalizeCustomJob) {
        return CustomJobRotationContext.builder()
                .withRotationJob(rotateCustomJob)
                .withRollbackJob(rollbackCustomJob)
                .withFinalizeJob(finalizeCustomJob)
                .build();
    }
}