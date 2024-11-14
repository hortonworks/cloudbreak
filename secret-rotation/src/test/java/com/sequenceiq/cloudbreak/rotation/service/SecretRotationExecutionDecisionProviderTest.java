package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretType;

@ExtendWith(MockitoExtension.class)
public class SecretRotationExecutionDecisionProviderTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:envCrn1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:resource1";

    @InjectMocks
    private SecretRotationExecutionDecisionProvider underTest;

    @ParameterizedTest
    @MethodSource("singleRotationRequests")
    void testSingleClusterRotation(RotationFlowExecutionType currentExecution, RotationFlowExecutionType requestExecution, boolean result) {
        assertEquals(result, underTest.executionRequired(new RotationMetadata(TestSecretType.TEST, currentExecution, requestExecution,
                "resourceCrn", null)));
    }

    static Stream<Arguments> singleRotationRequests() {
        return Stream.of(
                Arguments.of(PREVALIDATE, null, true),
                Arguments.of(ROTATE, null, true),
                Arguments.of(ROLLBACK, null, true),
                Arguments.of(FINALIZE, null, true),
                Arguments.of(PREVALIDATE, PREVALIDATE, true),
                Arguments.of(ROTATE, PREVALIDATE, false),
                Arguments.of(ROLLBACK, PREVALIDATE, false),
                Arguments.of(FINALIZE, PREVALIDATE, false),
                Arguments.of(PREVALIDATE, ROTATE, false),
                Arguments.of(ROTATE, ROTATE, true),
                Arguments.of(ROLLBACK, ROTATE, false),
                Arguments.of(FINALIZE, ROTATE, false),
                Arguments.of(PREVALIDATE, ROLLBACK, false),
                Arguments.of(ROTATE, ROLLBACK, false),
                Arguments.of(ROLLBACK, ROLLBACK, true),
                Arguments.of(FINALIZE, ROLLBACK, false),
                Arguments.of(PREVALIDATE, FINALIZE, false),
                Arguments.of(ROTATE, FINALIZE, false),
                Arguments.of(ROLLBACK, FINALIZE, false),
                Arguments.of(FINALIZE, FINALIZE, true)
        );
    }

    static Stream<Arguments> multiChildRotationRequests() {
        return Stream.of(
                Arguments.of(PREVALIDATE, null, true),
                Arguments.of(ROTATE, null, true),
                Arguments.of(ROLLBACK, null, true),
                Arguments.of(FINALIZE, null, true),
                Arguments.of(PREVALIDATE, PREVALIDATE, true),
                Arguments.of(ROTATE, PREVALIDATE, false),
                Arguments.of(ROLLBACK, PREVALIDATE, false),
                Arguments.of(FINALIZE, PREVALIDATE, false),
                Arguments.of(PREVALIDATE, ROTATE, false),
                Arguments.of(ROTATE, ROTATE, true),
                Arguments.of(ROLLBACK, ROTATE, false),
                Arguments.of(FINALIZE, ROTATE, false),
                Arguments.of(PREVALIDATE, ROLLBACK, false),
                Arguments.of(ROTATE, ROLLBACK, false),
                Arguments.of(ROLLBACK, ROLLBACK, true),
                Arguments.of(FINALIZE, ROLLBACK, false),
                Arguments.of(PREVALIDATE, FINALIZE, false),
                Arguments.of(ROTATE, FINALIZE, false),
                Arguments.of(ROLLBACK, FINALIZE, false),
                Arguments.of(FINALIZE, FINALIZE, true)
        );
    }
}
