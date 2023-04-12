package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType.HA;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType.NON_HA;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType.TWO_NODE_BASED;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.ScalingPath;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.configuration.AllowedScalingPaths;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
class FreeIpaScalingValidationServiceTest {

    @Mock
    private AllowedScalingPaths allowedScalingPaths;

    @InjectMocks
    private FreeIpaScalingValidationService underTest;

    @Test
    public void testUpscaleIfNoInstanceExistsThenValidationFails() {
        Stack stack = mock(Stack.class);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForUpscale(Set.of(), stack, new ScalingPath(NON_HA, HA)));
        assertEquals("There are no instances available for scaling!", exception.getMessage());
    }

    @Test
    public void testDownscaleIfNoInstanceExistsThenValidationFails() {
        Stack stack = mock(Stack.class);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForDownscale(Set.of(), stack, new ScalingPath(HA, NON_HA)));
        assertEquals("There are no instances available for scaling!", exception.getMessage());
    }

    private VerticalScaleRequest createVerticalScaleRequest() {
        VerticalScaleRequest request = new VerticalScaleRequest();
        request.setGroup("master");
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType("ec2bug");
        request.setTemplate(instanceTemplateRequest);
        return request;
    }

    @Test
    public void testUpscaleIfUnavailableInstanceExistsThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        validImSet.removeIf(instanceMetaData -> instanceMetaData.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY);
        InstanceMetaData pgw = createPrimaryGateway();
        validImSet.add(pgw);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForUpscale(validImSet, stack, new ScalingPath(NON_HA, HA)));
        assertEquals("Some of the instances is not available. Please fix them first! Instances: [pgw]", exception.getMessage());
    }

    @Test
    public void testDownscaleIfUnavailableInstanceExistsThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        validImSet.removeIf(instanceMetaData -> instanceMetaData.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY);
        InstanceMetaData pgw = createPrimaryGateway();
        validImSet.add(pgw);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, NON_HA)));
        assertEquals("Some of the instances is not available. Please fix them first! Instances: [pgw]", exception.getMessage());
    }

    @Test
    public void testUpscaleIfStackIsUnavailableThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(stack.isAvailable()).thenReturn(false);
        when(stack.getStackStatus()).thenReturn(createStackStatus(false));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForUpscale(validImSet, stack, new ScalingPath(NON_HA, HA)));
        assertEquals("Stack is not in available state, refusing to upscale. Current state: UNHEALTHY", exception.getMessage());
    }

    @Test
    public void testDownscaleIfStackIsUnavailableThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(stack.isAvailable()).thenReturn(false);
        when(stack.getStackStatus()).thenReturn(createStackStatus(false));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, NON_HA)));
        assertEquals("Stack is not in available state, refusing to downscale. Current state: UNHEALTHY", exception.getMessage());
    }

    @Test
    public void testUpscaleIfPathIsPermittedThenValidationPasses() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(stack.isAvailable()).thenReturn(true);
        when(allowedScalingPaths.getPaths()).thenReturn(Map.of(NON_HA, List.of(HA)));

        assertDoesNotThrow(() -> underTest.validateStackForUpscale(validImSet, stack, new ScalingPath(NON_HA, HA)));
    }

    @Test
    public void testDownscaleIfPathIsPermittedThenValidationPasses() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(3);
        when(stack.isAvailable()).thenReturn(true);
        when(allowedScalingPaths.getPaths()).thenReturn(Map.of(HA, List.of(TWO_NODE_BASED)));

        assertDoesNotThrow(() -> underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, TWO_NODE_BASED)));
    }

    @Test
    public void testUpscaleIfTargetNodeCountSmallerThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(3);

        assertThatCode(() -> underTest.validateStackForUpscale(validImSet, stack, new ScalingPath(HA, TWO_NODE_BASED)))
                .isExactlyInstanceOf(BadRequestException.class)
                .hasMessage("Refusing upscale as target node count is smaller than current. Current node count: 3, target node count: 2.");
    }

    @Test
    public void testDownscaleIfTargetNodeCountHigherThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);

        assertThatCode(() -> underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(TWO_NODE_BASED, HA)))
                .isExactlyInstanceOf(BadRequestException.class)
                .hasMessage("Refusing downscale as target node count is higher than current. Current node count: 2, target node count: 3.");
    }

    @Test
    public void testUpscaleIfPathIsNotPermittedAndNoAlternativeThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(stack.isAvailable()).thenReturn(true);
        when(allowedScalingPaths.getPaths()).thenReturn(Map.of());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForUpscale(validImSet, stack, new ScalingPath(TWO_NODE_BASED, HA)));
        assertEquals("Refusing upscale as scaling from 2 node to 3 is not supported.", exception.getMessage());
    }

    @Test
    public void testDownscaleIfPathIsNotPermittedAndNoAlternativeThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(stack.isAvailable()).thenReturn(true);
        when(allowedScalingPaths.getPaths()).thenReturn(Map.of());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, NON_HA)));
        assertEquals("Refusing downscale as scaling from 3 node to 1 is not supported.", exception.getMessage());
    }

    @Test
    public void testUpscaleIfPathNotPermittedAndAlternativeExistsThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(stack.isAvailable()).thenReturn(true);
        when(this.allowedScalingPaths.getPaths()).thenReturn(Map.of(NON_HA, List.of(TWO_NODE_BASED)));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForUpscale(validImSet, stack, new ScalingPath(NON_HA, HA)));
        assertEquals("Refusing upscale as scaling from 1 node to 3 is not supported. Supported upscale targets: [TWO_NODE_BASED]", exception.getMessage());
    }

    @Test
    public void testDownscaleIfPathNotPermittedAndAlternativeExistsThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(stack.isAvailable()).thenReturn(true);
        when(this.allowedScalingPaths.getPaths()).thenReturn(Map.of(HA, List.of(TWO_NODE_BASED)));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, NON_HA)));
        assertEquals("Refusing downscale as scaling from 3 node to 1 is not supported. Supported downscale targets: [TWO_NODE_BASED]", exception.getMessage());
    }

    @Test
    public void testUpscaleIfNodeCountWouldNotChangeThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(stack.isAvailable()).thenReturn(true);

        assertThatCode(() -> underTest.validateStackForUpscale(validImSet, stack, new ScalingPath(TWO_NODE_BASED, TWO_NODE_BASED)))
                .isExactlyInstanceOf(BadRequestException.class)
                .hasMessage("Refusing UPSCALE as the current node count already matches the node count of the requested availability type. Current " +
                        "node count: 2, target availability type: TWO_NODE_BASED and node count: 2.");
    }

    @ParameterizedTest
    @EnumSource(value = AvailabilityType.class)
    public void testDownscaleIfNodeCountWouldNotChangeThenValidationFails(AvailabilityType availabilityType) {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(availabilityType.getInstanceCount());
        when(stack.isAvailable()).thenReturn(true);

        assertThatCode(() -> underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(availabilityType, availabilityType)))
                .isExactlyInstanceOf(BadRequestException.class)
                .hasMessage(String.format("Refusing DOWNSCALE as the current node count already matches the node count of the requested availability type. " +
                        "Current node count: %d, target availability type: %s and node count: %d.",
                        availabilityType.getInstanceCount(), availabilityType.name(), availabilityType.getInstanceCount()));
    }

    private InstanceMetaData createPrimaryGateway() {
        InstanceMetaData pgw = new InstanceMetaData();
        pgw.setInstanceStatus(InstanceStatus.UNREACHABLE);
        pgw.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        pgw.setInstanceId("pgw");
        return pgw;
    }

    private Set<InstanceMetaData> createValidImSet(int instanceCount) {
        Set<InstanceMetaData> set = new HashSet<>();
        for (int i = 1; i <= instanceCount; i++) {
            InstanceMetaData im = new InstanceMetaData();
            if (i == 1) {
                im.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
                im.setInstanceId("pgw");
            } else {
                im.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
                im.setInstanceId("im" + i);
            }
            im.setInstanceStatus(InstanceStatus.CREATED);
            set.add(im);
        }
        return set;
    }

    private com.sequenceiq.freeipa.entity.StackStatus createStackStatus(boolean available) {
        com.sequenceiq.freeipa.entity.StackStatus stackStatus = new com.sequenceiq.freeipa.entity.StackStatus();
        stackStatus.setStatus(available ? Status.AVAILABLE : Status.UNHEALTHY);
        return stackStatus;
    }

}