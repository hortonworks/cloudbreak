package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
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
    void testExecutionTypeValidation(RotationFlowExecutionType requestedExecutionType, Set<RotationFlowExecutionType> executionTypesInDb,
            boolean errorExpected) {
        when(progressService.listSteps(any(), any())).thenReturn(executionTypesInDb.stream()
                .map(executionType -> new SecretRotationStepProgress(DATAHUB_CRN, TEST_SECRET, TEST_STEP, executionType, System.currentTimeMillis()))
                .toList());
        if (errorExpected) {
            assertThrows(CloudbreakServiceException.class, () -> underTest.validateExecutionType(DATAHUB_CRN, List.of(TEST_SECRET), requestedExecutionType));
        } else {
            assertDoesNotThrow(() -> underTest.validateExecutionType(DATAHUB_CRN, List.of(TEST_SECRET), requestedExecutionType));
        }
    }

    static Stream<Arguments> requests() {
        return Stream.of(
                Arguments.of(null, Set.of(), false),
                Arguments.of(null, Set.of(PREVALIDATE), true),
                Arguments.of(null, Set.of(PREVALIDATE, ROTATE), true),
                Arguments.of(null, Set.of(PREVALIDATE, ROTATE, ROLLBACK), true),
                Arguments.of(null, Set.of(PREVALIDATE, ROTATE, FINALIZE), true),
                Arguments.of(PREVALIDATE, Set.of(), false),
                Arguments.of(PREVALIDATE, Set.of(PREVALIDATE), true),
                Arguments.of(PREVALIDATE, Set.of(PREVALIDATE, ROTATE), true),
                Arguments.of(PREVALIDATE, Set.of(PREVALIDATE, ROTATE, ROLLBACK), true),
                Arguments.of(PREVALIDATE, Set.of(PREVALIDATE, ROTATE, FINALIZE), true),
                Arguments.of(ROTATE, Set.of(), true),
                Arguments.of(ROTATE, Set.of(PREVALIDATE), false),
                Arguments.of(ROTATE, Set.of(PREVALIDATE, ROTATE), true),
                Arguments.of(ROTATE, Set.of(PREVALIDATE, ROTATE, ROLLBACK), true),
                Arguments.of(ROTATE, Set.of(PREVALIDATE, ROTATE, FINALIZE), true),
                Arguments.of(ROLLBACK, Set.of(), true),
                Arguments.of(ROLLBACK, Set.of(PREVALIDATE), true),
                Arguments.of(ROLLBACK, Set.of(PREVALIDATE, ROTATE), false),
                Arguments.of(ROLLBACK, Set.of(PREVALIDATE, ROTATE, ROLLBACK), true),
                Arguments.of(ROLLBACK, Set.of(PREVALIDATE, ROTATE, FINALIZE), true),
                Arguments.of(FINALIZE, Set.of(), true),
                Arguments.of(FINALIZE, Set.of(PREVALIDATE), true),
                Arguments.of(FINALIZE, Set.of(PREVALIDATE, ROTATE), false),
                Arguments.of(FINALIZE, Set.of(PREVALIDATE, ROTATE, ROLLBACK), true),
                Arguments.of(FINALIZE, Set.of(PREVALIDATE, ROTATE, FINALIZE), true)
        );
    }
}
