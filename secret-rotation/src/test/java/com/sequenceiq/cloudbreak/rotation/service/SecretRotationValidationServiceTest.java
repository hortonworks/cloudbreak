package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgressStatus.FAILED;
import static com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgressStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretType;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;

@ExtendWith(MockitoExtension.class)
class SecretRotationValidationServiceTest {

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:resource1";

    private static final SecretType TEST_SECRET = TestSecretType.TEST;

    private static final SecretType TEST_SECRET_4 = TestSecretType.TEST_4;

    private static final TestSecretRotationStep TEST_STEP = TestSecretRotationStep.STEP;

    @Mock
    private SecretRotationStepProgressService secretRotationStepProgressService;

    @InjectMocks
    private SecretRotationValidationService underTest;

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
                Arguments.of(ROTATE, Optional.of(ROTATE), false),
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

    @Test
    void testWhenSecretIsInFailedRollbackStateThenOtherSecretRotationFails() {
        when(secretRotationStepProgressService.getProgressList(any()))
                .thenReturn(List.of(new SecretRotationStepProgress(DATAHUB_CRN, TEST_SECRET, TEST_STEP, ROLLBACK, FAILED)));

        Optional<RotationFlowExecutionType> rotationFlowExecutionType = underTest.validate(DATAHUB_CRN, List.of(TEST_SECRET), null, available());
        assertEquals(Optional.of(ROLLBACK), rotationFlowExecutionType);
    }

    @Test
    void testWhenSecretIsInFailedRollbackStateThenRollbackFailsWithOtherSecretType() {
        when(secretRotationStepProgressService.getProgressList(any()))
                .thenReturn(List.of(new SecretRotationStepProgress(DATAHUB_CRN, TEST_SECRET, TEST_STEP, ROLLBACK, FAILED)));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(DATAHUB_CRN, List.of(TestSecretType.TEST_2), null, available()));
        assertEquals("There is already a failed secret rotation for TEST secret type in ROLLBACK phase. " +
                "To resolve the issue please retry secret rotation.", exception.getMessage());
    }

    @Test
    public void testWhenStatusIsNotAvailable() {
        when(secretRotationStepProgressService.getProgressList(any())).thenReturn(List.of());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(DATAHUB_CRN, List.of(TEST_SECRET, TEST_SECRET_4), null, notAvailable()));
        assertEquals("The cluster must be in available state to start secret rotation.", exception.getMessage());
    }

    @Test
    public void testWhenStatusIsNotAvailableButStatusCheckNotNeeded() {
        when(secretRotationStepProgressService.getProgressList(any())).thenReturn(List.of());

        underTest.validate(DATAHUB_CRN, List.of(TEST_SECRET_4), null, notAvailable());
    }

    @Test
    void testWhenSecretIsInFailedRollbackStateThenRotateFailsWithTheSameSecretType() {
        when(secretRotationStepProgressService.getProgressList(any()))
                .thenReturn(List.of(new SecretRotationStepProgress(DATAHUB_CRN, TEST_SECRET, TEST_STEP, ROLLBACK, FAILED)));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validate(DATAHUB_CRN, List.of(TEST_SECRET), ROTATE, available()));
        assertEquals("There is already a failed secret rotation for TEST secret type in ROLLBACK phase. " +
                "To resolve the issue please retry secret rotation.", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("requests")
    void testExecutionTypeValidation(RotationFlowExecutionType requestedExecutionType, Optional<RotationFlowExecutionType> executionTypeInDb,
            boolean errorExpected) {
        when(secretRotationStepProgressService.getProgressList(any())).thenReturn(executionTypeInDb
                .map(executionType -> List.of(new SecretRotationStepProgress(DATAHUB_CRN, TEST_SECRET, TEST_STEP, executionType, IN_PROGRESS)))
                .orElse(List.of()));
        if (errorExpected) {
            assertThrows(BadRequestException.class, () -> underTest.validate(DATAHUB_CRN, List.of(TEST_SECRET), requestedExecutionType, available()));
        } else {
            assertDoesNotThrow(() -> underTest.validate(DATAHUB_CRN, List.of(TEST_SECRET), requestedExecutionType, available()));
        }
    }

    @Test
    void testValidateEnabledSecretTypesWhenEnabledSecretTypesIsNull() {
        assertDoesNotThrow(() -> underTest.validateEnabledSecretTypes(Set.of(TestSecretType.TEST), null));
    }

    @Test
    void testValidateEnabledSecretTypesWhenRequestedSecretTypesIsNull() {
        assertDoesNotThrow(() -> underTest.validateEnabledSecretTypes(null, null));
    }

    @Test
    void testValidateEnabledSecretTypesWhenRequestedSecretTypeIsInternal() {
        ReflectionTestUtils.setField(underTest, "enabledSecretTypes", List.of(TestSecretType.TEST));
        assertDoesNotThrow(() -> underTest.validateEnabledSecretTypes(Set.of(TestSecretType.TEST_3), null));
    }

    @Test
    void testValidateEnabledSecretTypesWhenSubRotation() {
        ReflectionTestUtils.setField(underTest, "enabledSecretTypes", List.of(TestSecretType.TEST));
        assertDoesNotThrow(() -> underTest.validateEnabledSecretTypes(Set.of(TestSecretType.TEST_2), ROTATE));
    }

    @Test
    void testValidateEnabledSecretTypesWhenRequestedSecretTypeIsNotEnabled() {
        ReflectionTestUtils.setField(underTest, "enabledSecretTypes", List.of(TestSecretType.TEST_2));
        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.validateEnabledSecretTypes(Set.of(TestSecretType.TEST), null));
        assertEquals("Secret types are not enabled: [TEST]", exception.getMessage());
    }

    @Test
    void testValidateEnabledSecretTypesWhenRequestedSecretTypeIsEnabled() {
        ReflectionTestUtils.setField(underTest, "enabledSecretTypes", List.of(TestSecretType.TEST));
        assertDoesNotThrow(() -> underTest.validateEnabledSecretTypes(Set.of(TestSecretType.TEST), null));
    }

    private Supplier<Boolean> available() {
        return () -> true;
    }

    private Supplier<Boolean> notAvailable() {
        return () -> false;
    }
}
