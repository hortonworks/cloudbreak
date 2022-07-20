package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.NODE_FAILURE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.TargetedUpscaleSupportService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.ScalabilityOption;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UpdateNodeCountValidatorTest {

    private static final String TEST_COMPUTE_GROUP = "compute";

    private static final String TEST_BLUEPRINT_TEXT = "blueprintText";

    private static final Optional<String> FORBIDDEN_DOWN = Optional.of("Requested scaling down is forbidden");

    private static final Optional<String> FORBIDDEN_UP = Optional.of("Requested scaling up is forbidden");

    private static final Optional<String> NOT_ENOUGH_NODE = Optional.of("You can not go under the minimal node count.");

    private static final Optional<String> NO_ERROR = Optional.empty();

    @InjectMocks
    public UpdateNodeCountValidator underTest;

    @Mock
    private TargetedUpscaleSupportService targetedUpscaleSupportService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @ParameterizedTest(name = "The master node count is {0} this will be scaled with {2} " +
            "node and the minimum is {1} the ScalabilityOption is {3}.")
    @MethodSource("testValidateScalabilityOfInstanceGroupData")
    public void testValidateScalabilityOfInstanceGroup(
            int instanceGroupNodeCount,
            int minimumNodeCount,
            int scalingNodeCount,
            ScalabilityOption scalabilityOption,
            Optional<String> errorMessageSegment) {
        StackDto stack = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentV4Request = mock(InstanceGroupAdjustmentV4Request.class);
        InstanceGroupDto instanceGroupDto = mock(InstanceGroupDto.class);
        InstanceGroupView instanceGroup = mock(InstanceGroupView.class);

        when(stack.getStack()).thenReturn(stackView);
        when(instanceGroupAdjustmentV4Request.getInstanceGroup()).thenReturn("master");
        when(instanceGroupAdjustmentV4Request.getScalingAdjustment()).thenReturn(scalingNodeCount);
        when(stack.getInstanceGroupByInstanceGroupName("master")).thenReturn(instanceGroupDto);
        when(instanceGroupDto.getInstanceGroup()).thenReturn(instanceGroup);
        when(stackView.getName()).thenReturn("master-stack");
        when(instanceGroup.getGroupName()).thenReturn("master");
        when(instanceGroup.getMinimumNodeCount()).thenReturn(minimumNodeCount);
        when(instanceGroupDto.getNodeCount()).thenReturn(instanceGroupNodeCount);
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
            when(stack.isAvailableWithStoppedInstances()).thenReturn(false);
        } else {
            when(stack.isAvailable()).thenReturn(false);
            when(stack.isAvailableWithStoppedInstances()).thenReturn(false);
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
        Assert.assertTrue(badRequestException.getMessage().contains(errorMessageSegment.get()));
    }

    private static Stream<Arguments> testValidateStatusForStartHostGroupData() {
        return Stream.of(
                Arguments.of(AVAILABLE, NO_ERROR),
                Arguments.of(NODE_FAILURE,
                        Optional.of("Data Hub 'master-stack' has 'NODE_FAILURE' state." +
                                " Node group start operation is not allowed for this state."))
        );
    }

    @Test
    public void testValidateInstanceGroupForStopStartIsSuccessful() {
        StackDto stack = mock(StackDto.class);
        setupMocksForStopStartInstanceGroupValidation(stack);
        assertDoesNotThrow(() -> underTest.validateInstanceGroupForStopStart(stack, "compute", 5));
    }

    @Test
    public void testValidateInstanceGroupForStopStartThrowsExceptionForUpscale() {
        StackDto stack = mock(StackDto.class);
        setupMocksForStopStartInstanceGroupValidation(stack);
        assertThatThrownBy(() -> underTest.validateInstanceGroupForStopStart(stack, "worker", 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Start instances operation is not allowed for worker host group.");
    }

    @Test
    public void testValidateInstanceGroupForStopStartThrowsExceptionForDownscale() {
        StackDto stack = mock(StackDto.class);
        setupMocksForStopStartInstanceGroupValidation(stack);
        assertThatThrownBy(() -> underTest.validateInstanceGroupForStopStart(stack, "worker", -1))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Stop instances operation is not allowed for worker host group.");
    }

    @Test
    public void testValidateInstanceGroupForStopStartThrowsExceptionForZeroScalingAdjustment() {
        StackDto stack = mock(StackDto.class);
        setupMocksForStopStartInstanceGroupValidation(stack);
        assertThatThrownBy(() -> underTest.validateInstanceGroupForStopStart(stack, "worker", 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Zero Scaling adjustment detected for worker host group.");
    }

    private void setupMocksForStopStartInstanceGroupValidation(StackDto stack) {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        Cluster cluster = mock(Cluster.class);
        Blueprint blueprint = mock(Blueprint.class);

        when(stack.getCluster()).thenReturn(cluster);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getBlueprintText()).thenReturn(TEST_BLUEPRINT_TEXT);
        when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getComputeHostGroups(any())).thenReturn(Set.of(TEST_COMPUTE_GROUP));
    }

    @Test
    void validateStackStatusUpscaleAvailable() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.AVAILABLE));
        underTest.validateStackStatus(stack, true);
    }

    @Test
    void validateStackStatusDownscaleAvailable() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.AVAILABLE));
        underTest.validateStackStatus(stack, false);
    }

    @Test
    void validateStackStatusUpscaleNodeFailureTargetedUpscaleSupported() {
        when(targetedUpscaleSupportService.targetedUpscaleEntitlementsEnabled(any())).thenReturn(Boolean.TRUE);
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.NODE_FAILURE));
        underTest.validateStackStatus(stack, true);
    }

    @Test
    void validateStackStatusDownscaleNodeFailureTargetedUpscaleSupported() {
        when(targetedUpscaleSupportService.targetedUpscaleEntitlementsEnabled(any())).thenReturn(Boolean.TRUE);
        Stack stack = new Stack();
        stack.setName("stack");
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.NODE_FAILURE));

        BadRequestException e = Assertions.assertThrows(BadRequestException.class,
                () -> underTest.validateStackStatus(stack, false));

        assertEquals("Data Hub 'stack' is currently in 'NODE_FAILURE' state. Node count can only be updated if it's running.", e.getMessage());
    }

    @Test
    void validateStackStatusUpscaleNodeFailureTargetedUpscaleNotSupported() throws Exception {
        when(targetedUpscaleSupportService.targetedUpscaleEntitlementsEnabled(any())).thenReturn(Boolean.FALSE);
        Stack stack = new Stack();
        stack.setName("stack");
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.NODE_FAILURE));

        BadRequestException e = Assertions.assertThrows(BadRequestException.class,
                () -> underTest.validateStackStatus(stack, true));

        assertEquals("Data Hub 'stack' is currently in 'NODE_FAILURE' state. Node count can only be updated if it's running.", e.getMessage());
    }

    @Test
    void validateStackStatusDownscaleNodeFailureTargetedUpscaleNotSupported() throws Exception {
        when(targetedUpscaleSupportService.targetedUpscaleEntitlementsEnabled(any())).thenReturn(Boolean.FALSE);
        Stack stack = new Stack();
        stack.setName("stack");
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.NODE_FAILURE));

        BadRequestException e = Assertions.assertThrows(BadRequestException.class,
                () -> underTest.validateStackStatus(stack, false));

        assertEquals("Data Hub 'stack' is currently in 'NODE_FAILURE' state. Node count can only be updated if it's running.", e.getMessage());
    }

    @Test
    void validateClusterStatusUpscaleAvailable() {
        Stack stack = new Stack();
        Cluster cluster = mock(Cluster.class);
        stack.setCluster(cluster);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.AVAILABLE));
        underTest.validateClusterStatus(stack, true);
    }

    @Test
    void validateClusterStatusDownscaleAvailable() {
        Stack stack = new Stack();
        Cluster cluster = mock(Cluster.class);
        stack.setCluster(cluster);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.AVAILABLE));
        underTest.validateClusterStatus(stack, false);
    }

    @Test
    void validateClusterStatusUpscaleNodeFailureTargetedUpscaleSupported() {
        when(targetedUpscaleSupportService.targetedUpscaleEntitlementsEnabled(any())).thenReturn(Boolean.TRUE);
        Stack stack = new Stack();
        Cluster cluster = mock(Cluster.class);
        stack.setCluster(cluster);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.NODE_FAILURE));
        underTest.validateClusterStatus(stack, true);
    }

    @Test
    void validateClusterStatusDownscaleNodeFailureTargetedUpscaleSupported() {
        when(targetedUpscaleSupportService.targetedUpscaleEntitlementsEnabled(any())).thenReturn(Boolean.TRUE);
        Stack stack = new Stack();
        stack.setName("stack");
        Cluster cluster = mock(Cluster.class);
        stack.setCluster(cluster);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.NODE_FAILURE));

        BadRequestException e = Assertions.assertThrows(BadRequestException.class,
                () -> underTest.validateClusterStatus(stack, false));

        assertEquals("Data Hub 'stack' is currently in 'NODE_FAILURE' state. Node count can only be updated if it's running.", e.getMessage());
    }

    @Test
    void validateClusterStatusUpscaleNodeFailureTargetedUpscaleNotSupported() {
        when(targetedUpscaleSupportService.targetedUpscaleEntitlementsEnabled(any())).thenReturn(Boolean.FALSE);
        Stack stack = new Stack();
        stack.setName("stack");
        Cluster cluster = mock(Cluster.class);
        stack.setCluster(cluster);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.NODE_FAILURE));

        BadRequestException e = Assertions.assertThrows(BadRequestException.class,
                () -> underTest.validateClusterStatus(stack, false));

        assertEquals("Data Hub 'stack' is currently in 'NODE_FAILURE' state. Node count can only be updated if it's running.", e.getMessage());
    }

    @Test
    void validateClusterStatusDownscaleNodeFailureTargetedUpscaleNotSupported() {
        when(targetedUpscaleSupportService.targetedUpscaleEntitlementsEnabled(any())).thenReturn(Boolean.FALSE);
        Stack stack = new Stack();
        stack.setName("stack");
        Cluster cluster = mock(Cluster.class);
        stack.setCluster(cluster);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.NODE_FAILURE));

        BadRequestException e = Assertions.assertThrows(BadRequestException.class,
                () -> underTest.validateClusterStatus(stack, false));

        assertEquals("Data Hub 'stack' is currently in 'NODE_FAILURE' state. Node count can only be updated if it's running.", e.getMessage());
    }
}