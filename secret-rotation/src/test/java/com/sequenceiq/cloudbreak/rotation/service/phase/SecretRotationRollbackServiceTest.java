package com.sequenceiq.cloudbreak.rotation.service.phase;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP2;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP3;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST;
import static com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgressStatus.FAILED;
import static com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgressStatus.FINISHED;
import static com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgressStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;

@ExtendWith(MockitoExtension.class)
public class SecretRotationRollbackServiceTest extends AbstractSecretRotationTest {

    private static final RotationMetadata METADATA = new RotationMetadata(TEST, ROLLBACK, null, "resource", Optional.empty(), null);

    @Mock
    private SecretRotationStepProgressService stepProgressService;

    @InjectMocks
    private SecretRotationRollbackService underTest;

    @Test
    public void testRollback() {
        doNothing().when(executor).executeRollback(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP, ROTATE, FAILED)));

        underTest.rollback(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(1)).executeRollback(any(), any());
        verify(stepProgressService, times(2)).update(any(), any(), any());
    }

    @Test
    public void testRollbackWhenStep3AlreadyInProgress() {
        doNothing().when(executor).executeRollback(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP3, ROLLBACK, IN_PROGRESS)));

        underTest.rollback(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(3)).executeRollback(any(), any());
        verify(stepProgressService, times(6)).update(any(), any(), any());
    }

    @Test
    public void testRollbackWhenStep2AlreadyInProgress() {
        doNothing().when(executor).executeRollback(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP2, ROLLBACK, IN_PROGRESS)));

        underTest.rollback(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(2)).executeRollback(any(), any());
        verify(stepProgressService, times(4)).update(any(), any(), any());
    }

    @Test
    public void testRollbackWhenStep1AlreadyInProgress() {
        doNothing().when(executor).executeRollback(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP, ROLLBACK, IN_PROGRESS)));

        underTest.rollback(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(1)).executeRollback(any(), any());
        verify(stepProgressService, times(2)).update(any(), any(), any());
    }

    @Test
    public void testRollbackWhenStep3AlreadyFinished() {
        doNothing().when(executor).executeRollback(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP3, ROLLBACK, FINISHED)));

        underTest.rollback(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(2)).executeRollback(any(), any());
        verify(stepProgressService, times(4)).update(any(), any(), any());
    }

    @Test
    public void testRollbackWhenStep2AlreadyFinished() {
        doNothing().when(executor).executeRollback(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP2, ROLLBACK, FINISHED)));

        underTest.rollback(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(1)).executeRollback(any(), any());
        verify(stepProgressService, times(2)).update(any(), any(), any());
    }

    @Test
    public void testRollbackWhenStep1AlreadyFinished() {
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP, ROLLBACK, FINISHED)));

        underTest.rollback(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(0)).executeRollback(any(), any());
        verify(stepProgressService, times(0)).update(any(), any(), any());
    }

    @Test
    public void testRollbackWhenStep3AlreadyFailed() {
        doNothing().when(executor).executeRollback(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP3, ROLLBACK, FAILED)));

        underTest.rollback(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(3)).executeRollback(any(), any());
        verify(stepProgressService, times(6)).update(any(), any(), any());
    }

    @Test
    public void testRollbackWhenProgressNotCorrect() {
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.FALSE);

        assertThrows(RuntimeException.class, () -> underTest.rollback(METADATA));

        verifyNoInteractions(contextProvider);
        verifyNoInteractions(executor);
    }

    @Override
    protected Object getUnderTest() {
        return underTest;
    }
}
