package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgressStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretType;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;

@ExtendWith(MockitoExtension.class)
public class SecretRotationValidationServiceTest {

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:resource1";

    private static final SecretType TEST_SECRET = TestSecretType.TEST;

    private static final TestSecretRotationStep TEST_STEP = TestSecretRotationStep.STEP;

    @Mock
    private SecretRotationStepProgressService progressService;

    @InjectMocks
    private SecretRotationValidationService underTest;

    @ParameterizedTest
    @MethodSource("requests")
    void testExecutionTypeValidation(RotationFlowExecutionType requestedExecutionType, Optional<RotationFlowExecutionType> executionTypeInDb,
            boolean errorExpected) {
        when(progressService.getProgress(any(), any())).thenReturn(executionTypeInDb
                .map(executionType -> new SecretRotationStepProgress(DATAHUB_CRN, TEST_SECRET, TEST_STEP, executionType, IN_PROGRESS)));
        if (errorExpected) {
            assertThrows(CloudbreakServiceException.class, () -> underTest.validateExecutionType(DATAHUB_CRN, List.of(TEST_SECRET), requestedExecutionType));
        } else {
            assertDoesNotThrow(() -> underTest.validateExecutionType(DATAHUB_CRN, List.of(TEST_SECRET), requestedExecutionType));
        }
    }

    static Stream<Arguments> requests() {
        return Stream.of(
                Arguments.of(null, Optional.empty(), false),
                Arguments.of(null, Optional.of(PREVALIDATE), true),
                Arguments.of(null, Optional.of(ROTATE), true),
                Arguments.of(null, Optional.of(ROLLBACK), true),
                Arguments.of(null, Optional.of(FINALIZE), true),
                Arguments.of(PREVALIDATE, Optional.empty(), false),
                Arguments.of(PREVALIDATE, Optional.of(PREVALIDATE), true),
                Arguments.of(PREVALIDATE, Optional.of(ROTATE), true),
                Arguments.of(PREVALIDATE, Optional.of(ROLLBACK), true),
                Arguments.of(PREVALIDATE, Optional.of(FINALIZE), true),
                Arguments.of(ROTATE, Optional.empty(), true),
                Arguments.of(ROTATE, Optional.of(PREVALIDATE), false),
                Arguments.of(ROTATE, Optional.of(ROTATE), true),
                Arguments.of(ROTATE, Optional.of(ROLLBACK), true),
                Arguments.of(ROTATE, Optional.of(FINALIZE), true),
                Arguments.of(ROLLBACK, Optional.empty(), true),
                Arguments.of(ROLLBACK, Optional.of(PREVALIDATE), true),
                Arguments.of(ROLLBACK, Optional.of(ROTATE), false),
                Arguments.of(ROLLBACK, Optional.of(ROLLBACK), true),
                Arguments.of(ROLLBACK, Optional.of(FINALIZE), true),
                Arguments.of(FINALIZE, Optional.empty(), true),
                Arguments.of(FINALIZE, Optional.of(PREVALIDATE), true),
                Arguments.of(FINALIZE, Optional.of(ROTATE), false),
                Arguments.of(FINALIZE, Optional.of(ROLLBACK), true),
                Arguments.of(FINALIZE, Optional.of(FINALIZE), true)
        );
    }
}
