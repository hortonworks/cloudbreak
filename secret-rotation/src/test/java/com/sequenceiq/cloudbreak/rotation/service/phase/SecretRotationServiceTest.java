package com.sequenceiq.cloudbreak.rotation.service.phase;

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

import java.util.Map;
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
public class SecretRotationServiceTest extends AbstractSecretRotationTest {

    private static final RotationMetadata METADATA = new RotationMetadata(TEST, ROTATE, null, "resource", Optional.empty());

    @Mock
    private SecretRotationStepProgressService stepProgressService;

    @InjectMocks
    private SecretRotationService underTest;

    @Test
    public void testRotateWhenContextMissing() {
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(contextProvider.getContexts(anyString())).thenReturn(Map.of());

        assertThrows(RuntimeException.class, () -> underTest.rotate(METADATA));

        verify(contextProvider).getContexts(anyString());
        verifyNoInteractions(executor);
    }

    @Test
    public void testRotate() {
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        doNothing().when(executor).executeRotate(any(), any());

        underTest.rotate(METADATA);

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(3)).executeRotate(any(), any());
        verify(stepProgressService, times(6)).update(any(), any(), any());
    }

    @Test
    public void testRotateWhenStep1AlreadyInProgress() {
        doNothing().when(executor).executeRotate(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP, ROTATE, IN_PROGRESS)));

        underTest.rotate(METADATA);

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(3)).executeRotate(any(), any());
        verify(stepProgressService, times(6)).update(any(), any(), any());
    }

    @Test
    public void testRotateWhenStep2AlreadyInProgress() {
        doNothing().when(executor).executeRotate(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP2, ROTATE, IN_PROGRESS)));

        underTest.rotate(METADATA);

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(2)).executeRotate(any(), any());
        verify(stepProgressService, times(4)).update(any(), any(), any());
    }

    @Test
    public void testRotateWhenStep1Finished() {
        doNothing().when(executor).executeRotate(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP, ROTATE, FINISHED)));

        underTest.rotate(METADATA);

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(2)).executeRotate(any(), any());
        verify(stepProgressService, times(4)).update(any(), any(), any());
    }

    @Test
    public void testRotateWhenStep2Finished() {
        doNothing().when(executor).executeRotate(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP2, ROTATE, FINISHED)));

        underTest.rotate(METADATA);

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(1)).executeRotate(any(), any());
        verify(stepProgressService, times(2)).update(any(), any(), any());
    }

    @Test
    public void testRotateWhenStep3Finished() {
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP3, ROTATE, FINISHED)));

        underTest.rotate(METADATA);

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(0)).executeRotate(any(), any());
        verify(stepProgressService, times(0)).update(any(), any(), any());
    }

    @Test
    public void testRotateWhenStep3AlreadyFailed() {
        doNothing().when(executor).executeRotate(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP3, ROTATE, FAILED)));

        underTest.rotate(METADATA);

        verify(contextProvider).getContexts(anyString());
        verify(executor, times(1)).executeRotate(any(), any());
        verify(stepProgressService, times(2)).update(any(), any(), any());
    }

    @Test
    public void testRotateWhenProgressNotCorrect() {
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.FALSE);

        assertThrows(RuntimeException.class, () -> underTest.rotate(METADATA));

        verifyNoInteractions(contextProvider);
        verifyNoInteractions(executor);
    }

    @Override
    protected Object getUnderTest() {
        return underTest;
    }
}
