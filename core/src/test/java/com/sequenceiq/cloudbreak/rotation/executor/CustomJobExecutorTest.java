package com.sequenceiq.cloudbreak.rotation.executor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.context.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;

@ExtendWith(MockitoExtension.class)
class CustomJobExecutorTest {

    @Mock
    private Runnable rotateCustomJob;

    @Mock
    private Runnable rollbackCustomJob;

    @Mock
    private Runnable finalizeCustomJob;

    @InjectMocks
    private CustomJobExecutor underTest;

    @Test
    public void testRotation() throws Exception {
        underTest.executeRotate(createContext(rotateCustomJob, null, null));

        verify(rotateCustomJob).run();
        verify(rollbackCustomJob, times(0)).run();
        verify(finalizeCustomJob, times(0)).run();
    }

    @Test
    public void testRotationFailure() throws CloudbreakRuntimeException {
        doThrow(new CloudbreakRuntimeException("something")).when(rotateCustomJob).run();

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRotate(createContext(rotateCustomJob, null, null)));

        verify(rotateCustomJob).run();
        verify(rollbackCustomJob, times(0)).run();
        verify(finalizeCustomJob, times(0)).run();
    }

    @Test
    public void testRollbackFailure() throws CloudbreakRuntimeException {
        doThrow(new CloudbreakRuntimeException("something")).when(rollbackCustomJob).run();

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRollback(createContext(rotateCustomJob, rollbackCustomJob, null)));

        verify(rotateCustomJob, times(0)).run();
        verify(rollbackCustomJob).run();
        verify(finalizeCustomJob, times(0)).run();
    }

    @Test
    public void testFinalization() throws Exception {
        underTest.executeFinalize(createContext(rotateCustomJob, null, finalizeCustomJob));

        verify(rotateCustomJob, times(0)).run();
        verify(rollbackCustomJob, times(0)).run();
        verify(finalizeCustomJob, times(1)).run();
    }

    @Test
    public void testFinalizationFailure() throws CloudbreakRuntimeException {
        doThrow(new CloudbreakRuntimeException("something")).when(finalizeCustomJob).run();

        assertThrows(SecretRotationException.class, () ->
                underTest.executeFinalize(createContext(rotateCustomJob, null, finalizeCustomJob)));

        verify(rotateCustomJob, times(0)).run();
        verify(rollbackCustomJob, times(0)).run();
        verify(finalizeCustomJob, times(1)).run();
    }

    private RotationContext createContext(Runnable rotateCustomJob, Runnable rollbackCustomJob, Runnable finalizeCustomJob) {
        return new CustomJobRotationContext.CustomJobRotationContextBuilder()
                .withRotationJob(rotateCustomJob)
                .withRollbackJob(rollbackCustomJob)
                .withFinalizeJob(finalizeCustomJob)
                .bulild();
    }

}