package com.sequenceiq.cloudbreak.rotation.service.progress;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.repository.SecretRotationStepProgressRepository;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@ExtendWith(MockitoExtension.class)
public class SecretRotationStepProgressServiceTest {

    private static final RotationMetadata METADATA = new RotationMetadata(TEST, ROTATE, null, "", Optional.empty());

    @Mock
    private SecretRotationStepProgressRepository repository;

    @InjectMocks
    private SecretRotationStepProgressService underTest;

    @Test
    public void testWhenNoProgress() {
        when(repository.findByResourceCrnAndExecutionTypeAndSecretTypeAndSecretRotationStep(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(new SecretRotationStepProgress());

        assertTrue(underTest.latestStep(METADATA, STEP).isPresent());

        verify(repository, times(1)).findByResourceCrnAndExecutionTypeAndSecretTypeAndSecretRotationStep(any(), any(), any(), any());
        verify(repository, times(1)).save(any());
    }

    @Test
    public void testWhenProgressPresent() {
        when(repository.findByResourceCrnAndExecutionTypeAndSecretTypeAndSecretRotationStep(any(), any(), any(), any())).thenReturn(Optional.of(
                new SecretRotationStepProgress("", TEST, STEP, ROTATE, null)));

        assertTrue(underTest.latestStep(METADATA, STEP).isPresent());

        verify(repository, times(1)).findByResourceCrnAndExecutionTypeAndSecretTypeAndSecretRotationStep(any(), any(), any(), any());
        verify(repository, times(0)).save(any());
    }
}
