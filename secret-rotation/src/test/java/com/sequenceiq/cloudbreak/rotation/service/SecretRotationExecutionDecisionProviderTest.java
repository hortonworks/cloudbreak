package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.rotation.MultiSecretType.DEMO_MULTI_SECRET;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST_2;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST_4;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretType;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResource;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.InterServiceMultiClusterRotationService;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.MultiClusterRotationService;

@ExtendWith(MockitoExtension.class)
public class SecretRotationExecutionDecisionProviderTest {

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:acc1:datalake:cluster1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:resource1";

    @Mock
    private MultiClusterRotationService trackingService;

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
        RotationMetadata metadata = new RotationMetadata(TEST_2, currentExecution, requestExecution, DATAHUB_CRN, Optional.of(DEMO_MULTI_SECRET));
        assertEquals(result, underTest.executionRequired(metadata));

        verifyNoInteractions(trackingService);
    }

    @ParameterizedTest
    @MethodSource("multiParentInitialRotationRequests")
    void testMultiClusterInitialParentRotation(RotationFlowExecutionType currentExecution, RotationFlowExecutionType requestExecution, boolean result) {
        MultiClusterRotationResourceType multiClusterRotationResourceType = MultiClusterRotationResourceType.INITIATED_PARENT;
        lenient().when(trackingService.getMultiRotationEntryForMetadata(any(), eq(multiClusterRotationResourceType))).thenReturn(Optional.empty());
        RotationMetadata metadata = new RotationMetadata(TEST_4, currentExecution, requestExecution,
                DATALAKE_CRN, Optional.of(DEMO_MULTI_SECRET));
        assertEquals(result, underTest.executionRequired(metadata));
    }

    @ParameterizedTest
    @MethodSource("multiParentFinalRotationRequests")
    void testMultiClusterFinalParentRotation(RotationFlowExecutionType currentExecution, RotationFlowExecutionType requestExecution, boolean result)
            throws IllegalAccessException {
        MultiClusterRotationResourceType multiClusterRotationResourceType = MultiClusterRotationResourceType.INITIATED_PARENT;
        lenient().when(trackingService.getMultiRotationEntryForMetadata(any(), eq(multiClusterRotationResourceType))).thenReturn(
                Optional.of(new MultiClusterRotationResource()));
        InterServiceMultiClusterRotationService interServiceMultiClusterRotationService =
                mock(InterServiceMultiClusterRotationService.class);
        FieldUtils.writeField(underTest, "interServiceMultiClusterRotationTrackingService",
                Optional.of(interServiceMultiClusterRotationService), true);
        lenient().when(interServiceMultiClusterRotationService.checkOngoingChildrenMultiSecretRotations(any(), any())).thenReturn(Boolean.FALSE);
        RotationMetadata met = new RotationMetadata(TEST_4, currentExecution, requestExecution,
                DATALAKE_CRN, Optional.of(DEMO_MULTI_SECRET));
        assertEquals(result, underTest.executionRequired(met));
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

    static Stream<Arguments> multiParentInitialRotationRequests() {
        return Stream.of(
                Arguments.of(PREVALIDATE, null, true),
                Arguments.of(ROTATE, null, true),
                Arguments.of(ROLLBACK, null, true),
                Arguments.of(FINALIZE, null, false),
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
                Arguments.of(FINALIZE, FINALIZE, false)
        );
    }

    static Stream<Arguments> multiParentFinalRotationRequests() {
        return Stream.of(
                Arguments.of(PREVALIDATE, null, false),
                Arguments.of(ROTATE, null, false),
                Arguments.of(ROLLBACK, null, false),
                Arguments.of(FINALIZE, null, true),
                Arguments.of(PREVALIDATE, PREVALIDATE, false),
                Arguments.of(ROTATE, PREVALIDATE, false),
                Arguments.of(ROLLBACK, PREVALIDATE, false),
                Arguments.of(FINALIZE, PREVALIDATE, false),
                Arguments.of(PREVALIDATE, ROTATE, false),
                Arguments.of(ROTATE, ROTATE, false),
                Arguments.of(ROLLBACK, ROTATE, false),
                Arguments.of(FINALIZE, ROTATE, false),
                Arguments.of(PREVALIDATE, ROLLBACK, false),
                Arguments.of(ROTATE, ROLLBACK, false),
                Arguments.of(ROLLBACK, ROLLBACK, false),
                Arguments.of(FINALIZE, ROLLBACK, false),
                Arguments.of(PREVALIDATE, FINALIZE, false),
                Arguments.of(ROTATE, FINALIZE, false),
                Arguments.of(ROLLBACK, FINALIZE, false),
                Arguments.of(FINALIZE, FINALIZE, true)
        );
    }
}
