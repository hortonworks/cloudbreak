package com.sequenceiq.cloudbreak.rotation.service.progress;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP2;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP3;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST;
import static com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgressStatus.FAILED;
import static com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgressStatus.FINISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgressStatus;
import com.sequenceiq.cloudbreak.rotation.repository.SecretRotationStepProgressRepository;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@ExtendWith(MockitoExtension.class)
public class SecretRotationStepProgressServiceTest {

    private static final RotationMetadata METADATA = new RotationMetadata(TEST, ROTATE, null, "", Optional.empty(), null);

    @Mock
    private SecretRotationStepProgressRepository repository;

    @InjectMocks
    private SecretRotationStepProgressService underTest;

    @Test
    void testUpdateIfExists() {
        SecretRotationStepProgress progress = new SecretRotationStepProgress();
        progress.setId(1L);
        when(repository.findByResourceCrnAndSecretType(any(), any())).thenReturn(Optional.of(progress));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SecretRotationStepProgress result = underTest.update(METADATA, STEP, SecretRotationStepProgressStatus.IN_PROGRESS);

        assertEquals(1, result.getId());
        assertEquals(STEP, result.getSecretRotationStep());
        assertEquals(SecretRotationStepProgressStatus.IN_PROGRESS, result.getStatus());
    }

    @Test
    void testUpdateIfNotExists() {
        when(repository.findByResourceCrnAndSecretType(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SecretRotationStepProgress result = underTest.update(METADATA, STEP, SecretRotationStepProgressStatus.IN_PROGRESS);

        assertNull(result.getId());
        assertEquals(STEP, result.getSecretRotationStep());
        assertEquals(SecretRotationStepProgressStatus.IN_PROGRESS, result.getStatus());
    }

    @ParameterizedTest
    @MethodSource("stepProgressInput")
    void testSingleClusterRotation(Optional<SecretRotationStepProgress> progressOptional, RotationFlowExecutionType currentExecution, boolean expected) {
        when(repository.findByResourceCrnAndSecretType(any(), any())).thenReturn(progressOptional);

        boolean result =
                underTest.executionValidByProgress(new RotationMetadata(null, currentExecution, null, null, Optional.empty(), null));

        assertEquals(expected, result);
    }

    static Stream<Arguments> stepProgressInput() {
        return Stream.of(
                Arguments.of(Optional.empty(), PREVALIDATE, true),
                Arguments.of(Optional.of(progress(PREVALIDATE, null, null, null)), PREVALIDATE, true),
                Arguments.of(Optional.of(progress(ROTATE, null, null, null)), PREVALIDATE, false),
                Arguments.of(Optional.empty(), ROTATE, false),
                Arguments.of(Optional.of(progress(PREVALIDATE, FINISHED, TEST, STEP3)), ROTATE, true),
                Arguments.of(Optional.of(progress(PREVALIDATE, FINISHED, TEST, STEP)), ROTATE, false),
                Arguments.of(Optional.of(progress(ROTATE, null, null, null)), ROTATE, true),
                Arguments.of(Optional.empty(), ROLLBACK, false),
                Arguments.of(Optional.of(progress(ROTATE, FAILED, TEST, STEP2)), ROLLBACK, true),
                Arguments.of(Optional.of(progress(ROTATE, FINISHED, TEST, STEP2)), ROLLBACK, false),
                Arguments.of(Optional.of(progress(ROTATE, FAILED, TEST, STEP)), ROLLBACK, true),
                Arguments.of(Optional.of(progress(ROLLBACK, null, null, null)), ROLLBACK, true),
                Arguments.of(Optional.empty(), FINALIZE, false),
                Arguments.of(Optional.of(progress(ROTATE, FINISHED, TEST, STEP3)), FINALIZE, true),
                Arguments.of(Optional.of(progress(ROTATE, FINISHED, TEST, STEP)), FINALIZE, false),
                Arguments.of(Optional.of(progress(FINALIZE, null, null, null)), FINALIZE, true)
        );
    }

    private static SecretRotationStepProgress progress(RotationFlowExecutionType executionType, SecretRotationStepProgressStatus status,
            SecretType secretType, SecretRotationStep step) {
        SecretRotationStepProgress progress = new SecretRotationStepProgress();
        progress.setCurrentExecutionType(executionType);
        progress.setStatus(status);
        progress.setSecretType(secretType);
        progress.setSecretRotationStep(step);
        return progress;
    }
}
