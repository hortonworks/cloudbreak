package com.sequenceiq.cloudbreak.rotation.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretType;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.repository.SecretRotationStepProgressRepository;

@ExtendWith(MockitoExtension.class)
public class SecretRotationStepProgressServiceTest {

    @Mock
    private SecretRotationStepProgressRepository repository;

    @InjectMocks
    private SecretRotationStepProgressService underTest;

    @BeforeEach
    public void mockRepository() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "repository", Optional.of(repository), true);
    }

    @Test
    public void testWhenNoProgress() {
        when(repository.findAllByResourceCrnAndExecutionType(any(), any())).thenReturn(Set.of());
        when(repository.save(any())).thenReturn(new SecretRotationStepProgress());

        assertTrue(underTest.latestStep("", TestSecretType.TEST, TestSecretRotationStep.STEP, RotationFlowExecutionType.ROTATE).isPresent());

        verify(repository, times(1)).findAllByResourceCrnAndExecutionType(any(), any());
        verify(repository, times(1)).save(any());
    }

    @Test
    public void testWhenProgressPresent() {
        when(repository.findAllByResourceCrnAndExecutionType(any(), any())).thenReturn(Set.of(
                new SecretRotationStepProgress("", TestSecretType.TEST, TestSecretRotationStep.STEP, RotationFlowExecutionType.ROTATE, null)));

        assertTrue(underTest.latestStep("", TestSecretType.TEST, TestSecretRotationStep.STEP, RotationFlowExecutionType.ROTATE).isPresent());

        verify(repository, times(1)).findAllByResourceCrnAndExecutionType(any(), any());
        verify(repository, times(0)).save(any());
    }
}
