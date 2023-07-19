package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.common.TestMultiSecretType;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretType;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.MultiClusterRotationMetadata;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.MultiClusterRotationTrackingService;

@ExtendWith(MockitoExtension.class)
public class SecretRotationExecutionDecisionProviderTest {

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:acc1:datalake:cluster1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:resource1";

    private static final MultiSecretType MULTI_SECRET_TYPE = TestMultiSecretType.MULTI_TEST;

    @Mock
    private MultiClusterRotationTrackingService trackingService;

    @InjectMocks
    private SecretRotationExecutionDecisionProvider underTest;

    @ParameterizedTest
    @MethodSource("singleRotationRequests")
    void testSingleClusterRotation(RotationFlowExecutionType currentExecution, RotationFlowExecutionType requestExecution, boolean result) {
        assertEquals(result, underTest.executionRequired(new RotationMetadata(TestSecretType.TEST, currentExecution, requestExecution,
                "resourceCrn", Optional.empty())));

        verifyNoInteractions(trackingService);
    }

    @ParameterizedTest
    @MethodSource("multiChildRotationRequests")
    void testMultiClusterChildRotation(RotationFlowExecutionType currentExecution, RotationFlowExecutionType requestExecution, boolean result) {
        MultiClusterRotationResourceType multiClusterRotationResourceType = MultiClusterRotationResourceType.CHILD;
        lenient().when(trackingService.multiRotationNeededForResource(any(), eq(multiClusterRotationResourceType))).thenReturn(Boolean.TRUE);
        Optional<MultiClusterRotationMetadata> multiClusterRotationMetadata = Optional.of(getMultiClusterRotationMetadata());
        RotationMetadata metadata = new RotationMetadata(MULTI_SECRET_TYPE.childSecretType(), currentExecution, requestExecution,
                DATAHUB_CRN, multiClusterRotationMetadata);
        assertEquals(result, underTest.executionRequired(metadata));
    }

    @ParameterizedTest
    @MethodSource("multiChildRotationRequests")
    void testMultiClusterNonExistingChildRotation(RotationFlowExecutionType currentExecution, RotationFlowExecutionType requestExecution, boolean result) {
        lenient().when(trackingService.multiRotationNeededForResource(any(), any())).thenReturn(Boolean.FALSE);
        Optional<MultiClusterRotationMetadata> multiClusterRotationMetadata = Optional.of(getMultiClusterRotationMetadata());
        RotationMetadata metadata = new RotationMetadata(MULTI_SECRET_TYPE.childSecretType(), currentExecution, requestExecution,
                DATAHUB_CRN, multiClusterRotationMetadata);
        assertFalse(underTest.executionRequired(metadata));
    }

    @ParameterizedTest
    @MethodSource("multiParentInitialRotationRequests")
    void testMultiClusterInitialParentRotation(RotationFlowExecutionType currentExecution, RotationFlowExecutionType requestExecution, boolean result) {
        MultiClusterRotationResourceType multiClusterRotationResourceType = MultiClusterRotationResourceType.PARENT_INITIAL;
        lenient().when(trackingService.multiRotationNeededForResource(any(), eq(multiClusterRotationResourceType))).thenReturn(Boolean.TRUE);
        Optional<MultiClusterRotationMetadata> multiClusterRotationMetadata = Optional.of(getMultiClusterRotationMetadata());
        RotationMetadata metadata = new RotationMetadata(MULTI_SECRET_TYPE.parentSecretType(), currentExecution, requestExecution,
                DATALAKE_CRN, multiClusterRotationMetadata);
        assertEquals(result, underTest.executionRequired(metadata));
    }

    @ParameterizedTest
    @MethodSource("multiParentFinalRotationRequests")
    void testMultiClusterFinalParentRotation(RotationFlowExecutionType currentExecution, RotationFlowExecutionType requestExecution, boolean result) {
        MultiClusterRotationResourceType multiClusterRotationResourceType = MultiClusterRotationResourceType.PARENT_FINAL;
        lenient().when(trackingService.multiRotationNeededForResource(any(), eq(multiClusterRotationResourceType))).thenReturn(Boolean.TRUE);
        Optional<MultiClusterRotationMetadata> multiClusterRotationMetadata = Optional.of(getMultiClusterRotationMetadata());
        RotationMetadata met = new RotationMetadata(MULTI_SECRET_TYPE.parentSecretType(), currentExecution, requestExecution,
                DATALAKE_CRN, multiClusterRotationMetadata);
        assertEquals(result, underTest.executionRequired(met));
    }

    private static MultiClusterRotationMetadata getMultiClusterRotationMetadata() {
        return new MultiClusterRotationMetadata(DATALAKE_CRN, Set.of(DATAHUB_CRN), TestMultiSecretType.MULTI_TEST);
    }

    static Stream<Arguments> singleRotationRequests() {
        return Stream.of(
                Arguments.of(ROTATE, null, true),
                Arguments.of(ROLLBACK, null, true),
                Arguments.of(FINALIZE, null, true),
                Arguments.of(ROTATE, ROTATE, true),
                Arguments.of(ROLLBACK, ROTATE, false),
                Arguments.of(FINALIZE, ROTATE, false),
                Arguments.of(ROTATE, ROLLBACK, false),
                Arguments.of(ROLLBACK, ROLLBACK, true),
                Arguments.of(FINALIZE, ROLLBACK, false),
                Arguments.of(ROTATE, FINALIZE, false),
                Arguments.of(ROLLBACK, FINALIZE, false),
                Arguments.of(FINALIZE, FINALIZE, true)
        );
    }

    static Stream<Arguments> multiChildRotationRequests() {
        return Stream.of(
                Arguments.of(ROTATE, null, true),
                Arguments.of(ROLLBACK, null, true),
                Arguments.of(FINALIZE, null, true),
                Arguments.of(ROTATE, ROTATE, true),
                Arguments.of(ROLLBACK, ROTATE, false),
                Arguments.of(FINALIZE, ROTATE, false),
                Arguments.of(ROTATE, ROLLBACK, false),
                Arguments.of(ROLLBACK, ROLLBACK, true),
                Arguments.of(FINALIZE, ROLLBACK, false),
                Arguments.of(ROTATE, FINALIZE, false),
                Arguments.of(ROLLBACK, FINALIZE, false),
                Arguments.of(FINALIZE, FINALIZE, true)
        );
    }

    static Stream<Arguments> multiParentInitialRotationRequests() {
        return Stream.of(
                Arguments.of(ROTATE, null, true),
                Arguments.of(ROLLBACK, null, true),
                Arguments.of(FINALIZE, null, false),
                Arguments.of(ROTATE, ROTATE, true),
                Arguments.of(ROLLBACK, ROTATE, false),
                Arguments.of(FINALIZE, ROTATE, false),
                Arguments.of(ROTATE, ROLLBACK, false),
                Arguments.of(ROLLBACK, ROLLBACK, true),
                Arguments.of(FINALIZE, ROLLBACK, false),
                Arguments.of(ROTATE, FINALIZE, false),
                Arguments.of(ROLLBACK, FINALIZE, false),
                Arguments.of(FINALIZE, FINALIZE, false)
        );
    }

    static Stream<Arguments> multiParentFinalRotationRequests() {
        return Stream.of(
                Arguments.of(ROTATE, null, false),
                Arguments.of(ROLLBACK, null, false),
                Arguments.of(FINALIZE, null, true),
                Arguments.of(ROTATE, ROTATE, false),
                Arguments.of(ROLLBACK, ROTATE, false),
                Arguments.of(FINALIZE, ROTATE, false),
                Arguments.of(ROTATE, ROLLBACK, false),
                Arguments.of(ROLLBACK, ROLLBACK, false),
                Arguments.of(FINALIZE, ROLLBACK, false),
                Arguments.of(ROTATE, FINALIZE, false),
                Arguments.of(ROLLBACK, FINALIZE, false),
                Arguments.of(FINALIZE, FINALIZE, true)
        );
    }
}
