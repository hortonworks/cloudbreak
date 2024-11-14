package com.sequenceiq.cloudbreak.rotation.service.phase;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
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
public class SecretRotationPreValidateServiceTest extends AbstractSecretRotationTest {

    private static final RotationMetadata METADATA = new RotationMetadata(TEST, PREVALIDATE, null, "resource", null);

    @Mock
    private SecretRotationStepProgressService stepProgressService;

    @InjectMocks
    private SecretRotationPreValidateService underTest;

    @Test
    public void testPreValidateWhenContextMissing() {
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(contextProvider.getContextsWithProperties(anyString(), any())).thenReturn(Map.of());

        assertThrows(RuntimeException.class, () -> underTest.preValidate(METADATA));

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verifyNoInteractions(executor);
    }

    @Test
    public void testPreValidate() {
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        doNothing().when(executor).executePreValidation(any(), any());
        when(stepProgressService.getProgress(any())).thenReturn(Optional.empty());

        underTest.preValidate(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(3)).executePreValidation(any(), any());
        verify(stepProgressService, times(6)).update(any(), any(), any());
    }

    @Test
    public void testPreValidateWhenStep1AlreadyInProgress() {
        doNothing().when(executor).executePreValidation(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP, PREVALIDATE, IN_PROGRESS)));

        underTest.preValidate(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(3)).executePreValidation(any(), any());
        verify(stepProgressService, times(6)).update(any(), any(), any());
    }

    @Test
    public void testPreValidateWhenStep2AlreadyInProgress() {
        doNothing().when(executor).executePreValidation(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP2, PREVALIDATE, IN_PROGRESS)));

        underTest.preValidate(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(2)).executePreValidation(any(), any());
        verify(stepProgressService, times(4)).update(any(), any(), any());
    }

    @Test
    public void testPreValidateWhenStep1Finished() {
        doNothing().when(executor).executePreValidation(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP, PREVALIDATE, FINISHED)));

        underTest.preValidate(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(2)).executePreValidation(any(), any());
        verify(stepProgressService, times(4)).update(any(), any(), any());
    }

    @Test
    public void testPreValidateWhenStep2Finished() {
        doNothing().when(executor).executePreValidation(any(), any());
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP2, PREVALIDATE, FINISHED)));

        underTest.preValidate(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(1)).executePreValidation(any(), any());
        verify(stepProgressService, times(2)).update(any(), any(), any());
    }

    @Test
    public void testPreValidateWhenStep3Finished() {
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP3, PREVALIDATE, FINISHED)));

        underTest.preValidate(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(0)).executePreValidation(any(), any());
        verify(stepProgressService, times(0)).update(any(), any(), any());
    }

    @Test
    public void testPreValidateWhenStep3AlreadyFailed() {
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.TRUE);
        when(stepProgressService.getProgress(any())).thenReturn(
                Optional.of(new SecretRotationStepProgress(null, TEST, STEP3, PREVALIDATE, FAILED)));

        underTest.preValidate(METADATA);

        verify(contextProvider).getContextsWithProperties(anyString(), any());
        verify(executor, times(1)).executePreValidation(any(), any());
        verify(stepProgressService, times(2)).update(any(), any(), any());
    }

    @Test
    public void testPreValidateWhenProgressNotCorrect() {
        when(stepProgressService.executionValidByProgress(any())).thenReturn(Boolean.FALSE);

        assertThrows(RuntimeException.class, () -> underTest.preValidate(METADATA));

        verifyNoInteractions(contextProvider);
        verifyNoInteractions(executor);
    }

    @Override
    protected Object getUnderTest() {
        return underTest;
    }
}
