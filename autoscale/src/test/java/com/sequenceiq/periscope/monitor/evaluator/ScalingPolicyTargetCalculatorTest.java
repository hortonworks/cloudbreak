package com.sequenceiq.periscope.monitor.evaluator;

import static com.sequenceiq.periscope.api.model.AdjustmentType.EXACT;
import static com.sequenceiq.periscope.api.model.AdjustmentType.NODE_COUNT;
import static com.sequenceiq.periscope.api.model.AdjustmentType.PERCENTAGE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.Before;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;

@ExtendWith(MockitoExtension.class)
public class ScalingPolicyTargetCalculatorTest {

    private static final Integer TEST_HOSTGROUP_MIN_SIZE = 0;

    @InjectMocks
    private ScalingPolicyTargetCalculator underTest;

    private ScalingPolicy scalingPolicyMock = mock(ScalingPolicy.class);

    private ScalingEvent scalingEventMock = mock(ScalingEvent.class);

    public static Stream<Arguments> policyScalingAdjustments() {
        return Stream.of(
                //TestCase, AdjustmentType, CurrentHostGroupCount,ScalingAdjustment,ExpectedScalingCount
                Arguments.of("SCALING_POLICY_NODE_COUNT", NODE_COUNT, 2, 25, 27),
                Arguments.of("SCALING_POLICY_NODE_COUNT", NODE_COUNT, 2, 198, 200),

                Arguments.of("SCALING_POLICY_PERCENTAGE", PERCENTAGE, 2, 50, 3),
                Arguments.of("SCALING_POLICY_PERCENTAGE", PERCENTAGE, 2, 100, 4),

                Arguments.of("SCALING_POLICY_EXACT", EXACT, 2, 10, 10),
                Arguments.of("SCALING_POLICY_EXACT", EXACT, 12, 24, 24),

                Arguments.of("SCALING_POLICY_NODE_COUNT_BEYOND_MIN_LIMIT", NODE_COUNT, 2, -12, TEST_HOSTGROUP_MIN_SIZE),
                Arguments.of("SCALING_POLICY_PERCENTAGE_BEYOND_MIN_LIMIT", PERCENTAGE, 2, -100, TEST_HOSTGROUP_MIN_SIZE),
                Arguments.of("SCALING_POLICY_EXACT_BEYOND_MIN_LIMIT", EXACT, 2, 0, TEST_HOSTGROUP_MIN_SIZE),

                Arguments.of("SCALING_POLICY_NODE_COUNT_WITHIN_MIN_LIMIT", NODE_COUNT, 10, -2, 8),
                Arguments.of("SCALING_POLICY_PERCENTAGE_WITHIN_MIN_LIMIT", PERCENTAGE, 10, -50, 5),
                Arguments.of("SCALING_POLICY_EXACT_WITHIN_MIN_LIMIT", EXACT, 10, 6, 6)
        );
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @ParameterizedTest(name = "{0}: With AdjustmentType{1}, currentHostGroupCount={2}, scalingAdjustment ={3}, expectedScalingCount={4} ")
    @MethodSource("policyScalingAdjustments")
    public void testPolicyScalingCalculation(String testType, AdjustmentType adjustmentType, int currentHostGroupCount,
            int scalingAdjustment, int expectedScalingCount) {
        TimeAlert timeAlertMock = mock(TimeAlert.class);
        validateTargetCalculation(timeAlertMock, adjustmentType, currentHostGroupCount, scalingAdjustment, expectedScalingCount);
    }

    private void validateTargetCalculation(BaseAlert baseAlertMock, AdjustmentType adjustmentType,
            int currentHostGroupCount, int scalingAdjument, int expectedScaleUpCount) {
        MockitoAnnotations.initMocks(this);
        String testHostGroup = "compute";

        when(scalingEventMock.getAlert()).thenReturn(baseAlertMock);
        when(baseAlertMock.getScalingPolicy()).thenReturn(scalingPolicyMock);

        when(scalingPolicyMock.getAdjustmentType()).thenReturn(adjustmentType);
        when(scalingPolicyMock.getHostGroup()).thenReturn(testHostGroup);
        when(scalingPolicyMock.getScalingAdjustment()).thenReturn(scalingAdjument);

        int desiredNodeCount = underTest.getDesiredAbsoluteNodeCount(scalingEventMock, currentHostGroupCount);
        assertEquals("Desired NodeCount should match", expectedScaleUpCount, desiredNodeCount);
    }
}
