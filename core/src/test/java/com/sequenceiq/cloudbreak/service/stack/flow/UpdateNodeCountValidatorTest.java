package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.NODE_FAILURE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.common.api.type.ScalabilityOption;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UpdateNodeCountValidatorTest {

    private static final Optional<String> FORBIDDEN_DOWN = Optional.of("Requested scaling down is forbidden");

    private static final Optional<String> FORBIDDEN_UP = Optional.of("Requested scaling up is forbidden");

    private static final Optional<String> NOT_ENOUGH_NODE = Optional.of("You can not go under the minimal node count.");

    private static final Optional<String> NO_ERROR = Optional.empty();

    @InjectMocks
    public UpdateNodeCountValidator underTest;

    @ParameterizedTest(name = "The master node count is {0} this will be scaled with {2} " +
            "node and the minimum is {1} the ScalabilityOption is {3}.")
    @MethodSource("testValidateScalabilityOfInstanceGroupData")
    public void testValidateScalabilityOfInstanceGroup(
            int instanceGroupNodeCount,
            int minimumNodeCount,
            int scalingNodeCount,
            ScalabilityOption scalabilityOption,
            Optional<String> errorMessageSegment) {
        Stack stack = mock(Stack.class);
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentV4Request = mock(InstanceGroupAdjustmentV4Request.class);
        InstanceGroup instanceGroup = mock(InstanceGroup.class);

        when(instanceGroupAdjustmentV4Request.getInstanceGroup()).thenReturn("master");
        when(instanceGroupAdjustmentV4Request.getScalingAdjustment()).thenReturn(scalingNodeCount);
        when(stack.getInstanceGroupByInstanceGroupName("master")).thenReturn(instanceGroup);
        when(stack.getName()).thenReturn("master-stack");
        when(instanceGroup.getGroupName()).thenReturn("master");
        when(instanceGroup.getMinimumNodeCount()).thenReturn(minimumNodeCount);
        when(instanceGroup.getNodeCount()).thenReturn(instanceGroupNodeCount);
        when(instanceGroup.getScalabilityOption()).thenReturn(scalabilityOption);

        if (errorMessageSegment.isPresent()) {
            BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
                underTest.validateScalabilityOfInstanceGroup(stack, instanceGroupAdjustmentV4Request);
            });
            Assert.assertTrue(badRequestException.getMessage().contains(errorMessageSegment.get()));
        } else {
            assertDoesNotThrow(() -> underTest.validateScalabilityOfInstanceGroup(stack, instanceGroupAdjustmentV4Request));
        }
    }

    private static Stream<Arguments> testValidateScalabilityOfInstanceGroupData() {
        return Stream.of(
                Arguments.of(3, 3, -1, ScalabilityOption.ALLOWED, NOT_ENOUGH_NODE),
                Arguments.of(3, 3, -1, ScalabilityOption.ONLY_DOWNSCALE, NOT_ENOUGH_NODE),
                Arguments.of(3, 3, -1, ScalabilityOption.ONLY_UPSCALE, NOT_ENOUGH_NODE),
                Arguments.of(3, 3, -1, ScalabilityOption.FORBIDDEN, NOT_ENOUGH_NODE),
                Arguments.of(3, 2, -1, ScalabilityOption.ALLOWED, NO_ERROR),
                Arguments.of(3, 2, -1, ScalabilityOption.ONLY_DOWNSCALE, NO_ERROR),
                Arguments.of(3, 2, -1, ScalabilityOption.ONLY_UPSCALE, FORBIDDEN_DOWN),
                Arguments.of(3, 2, -1, ScalabilityOption.FORBIDDEN, FORBIDDEN_DOWN),
                Arguments.of(3, 2, 1, ScalabilityOption.ALLOWED, NO_ERROR),
                Arguments.of(3, 2, 1, ScalabilityOption.ONLY_DOWNSCALE, FORBIDDEN_UP),
                Arguments.of(3, 2, 1, ScalabilityOption.ONLY_UPSCALE, NO_ERROR),
                Arguments.of(3, 2, 1, ScalabilityOption.FORBIDDEN, FORBIDDEN_UP),
                Arguments.of(3, 2, 2, ScalabilityOption.ALLOWED, NO_ERROR),
                Arguments.of(3, 2, 2, ScalabilityOption.ONLY_DOWNSCALE, FORBIDDEN_UP),
                Arguments.of(3, 2, 2, ScalabilityOption.ONLY_UPSCALE, NO_ERROR),
                Arguments.of(3, 2, 2, ScalabilityOption.FORBIDDEN, FORBIDDEN_UP),
                Arguments.of(3, 0, 1, ScalabilityOption.ALLOWED, NO_ERROR),
                Arguments.of(3, 0, 1, ScalabilityOption.ONLY_DOWNSCALE, FORBIDDEN_UP),
                Arguments.of(3, 0, 1, ScalabilityOption.ONLY_UPSCALE, NO_ERROR),
                Arguments.of(3, 0, 1, ScalabilityOption.FORBIDDEN, FORBIDDEN_UP)
        );
    }

    @ParameterizedTest(name = "The stack status is {0}.")
    @MethodSource("testValidateStatusForStartHostGroupData")
    public void testValidateStatusForStartHostGroup(
            Status status,
            Optional<String> errorMessageSegment) {
        Stack stack = mock(Stack.class);
        Cluster cluster = mock(Cluster.class);
        when(stack.getName()).thenReturn("master-stack");
        when(stack.getStatus()).thenReturn(status);
        when(stack.getCluster()).thenReturn(cluster);
        when(cluster.getName()).thenReturn("master-stack");
        if (status == AVAILABLE) {
            when(stack.isAvailable()).thenReturn(true);
            when(stack.hasNodeFailure()).thenReturn(false);
        } else if (status == NODE_FAILURE) {
            when(stack.isAvailable()).thenReturn(false);
            when(stack.hasNodeFailure()).thenReturn(true);
        } else {
            when(stack.isAvailable()).thenReturn(false);
            when(stack.hasNodeFailure()).thenReturn(false);
        }

        if (errorMessageSegment.isPresent()) {
            checkExecutableThrowsException(errorMessageSegment, stack, () -> underTest.validateStackStatusForStartHostGroup(stack));
            checkExecutableThrowsException(errorMessageSegment, stack, () -> underTest.validateClusterStatusForStartHostGroup(stack));
        } else {
            assertDoesNotThrow(() -> underTest.validateStackStatusForStartHostGroup(stack));
            assertDoesNotThrow(() -> underTest.validateClusterStatusForStartHostGroup(stack));
        }
    }

    private void checkExecutableThrowsException(Optional<String> errorMessageSegment, Stack stack, Executable executable) {
        BadRequestException badRequestException = assertThrows(BadRequestException.class, executable);
        Assert.assertEquals(errorMessageSegment.get(), badRequestException.getMessage());
    }

    private static Stream<Arguments> testValidateStatusForStartHostGroupData() {
        return Stream.of(
                Arguments.of(AVAILABLE, NO_ERROR),
                Arguments.of(NODE_FAILURE, NO_ERROR),
                Arguments.of(DELETE_IN_PROGRESS,
                        Optional.of("Data Hub 'master-stack' has 'DELETE_IN_PROGRESS' state." +
                                " Node group start operation is not allowed for this state."))
        );
    }
}