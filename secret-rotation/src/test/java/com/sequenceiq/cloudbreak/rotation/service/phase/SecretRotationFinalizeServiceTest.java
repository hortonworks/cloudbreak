package com.sequenceiq.cloudbreak.rotation.service.phase;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
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
public class SecretRotationFinalizeServiceTest extends AbstractSecretRotationTest {

    private static final RotationMetadata METADATA = new RotationMetadata(TEST, FINALIZE, null, "resource", Optional.empty(), null);

    @Mock
    private SecretRotationStepProgressService stepProgressService;

    @InjectMocks
    private SecretRotationFinalizeService underTest;

    @Test
    public void testFinalize() {
        doNothing().when(executor).executeFinalize(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP3, ROTATE, FINISHED)));

        underTest.finalize(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(3)).executeFinalize(any(), any());
        verify(stepProgressService, times(6)).update(any(), any(), any());
    }

    @Test
    public void testFinalizeWhenStep3AlreadyInProgress() {
        doNothing().when(executor).executeFinalize(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP3, FINALIZE, IN_PROGRESS)));

        underTest.finalize(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(3)).executeFinalize(any(), any());
        verify(stepProgressService, times(6)).update(any(), any(), any());
    }

    @Test
    public void testFinalizeWhenStep2AlreadyInProgress() {
        doNothing().when(executor).executeFinalize(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP2, FINALIZE, IN_PROGRESS)));

        underTest.finalize(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(2)).executeFinalize(any(), any());
        verify(stepProgressService, times(4)).update(any(), any(), any());
    }

    @Test
    public void testFinalizeWhenStep1AlreadyInProgress() {
        doNothing().when(executor).executeFinalize(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP, FINALIZE, IN_PROGRESS)));

        underTest.finalize(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(1)).executeFinalize(any(), any());
        verify(stepProgressService, times(2)).update(any(), any(), any());
    }

    @Test
    public void testFinalizeWhenStep3AlreadyFinished() {
        doNothing().when(executor).executeFinalize(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP3, FINALIZE, FINISHED)));

        underTest.finalize(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(2)).executeFinalize(any(), any());
        verify(stepProgressService, times(4)).update(any(), any(), any());
    }

    @Test
    public void testFinalizeWhenStep2AlreadyFinished() {
        doNothing().when(executor).executeFinalize(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP2, FINALIZE, FINISHED)));

        underTest.finalize(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(1)).executeFinalize(any(), any());
        verify(stepProgressService, times(2)).update(any(), any(), any());
    }

    @Test
    public void testFinalizeWhenStep1AlreadyFinished() {
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP, FINALIZE, FINISHED)));

        underTest.finalize(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(0)).executeFinalize(any(), any());
        verify(stepProgressService, times(0)).update(any(), any(), any());
    }

    @Test
    public void testFinalizeWhenStep3AlreadyFailed() {
        doNothing().when(executor).executeFinalize(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP3, FINALIZE, FAILED)));

        underTest.finalize(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(3)).executeFinalize(any(), any());
        verify(stepProgressService, times(6)).update(any(), any(), any());
    }

    @Test
    public void testFinalizeWhenProgressNotCorrect() {
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.FALSE);

        assertThrows(RuntimeException.class, () -> underTest.finalize(METADATA));

        verifyNoInteractions(contextProvider);
        verifyNoInteractions(executor);
    }

    @Override
    protected Object getUnderTest() {
        return underTest;
    }
}
