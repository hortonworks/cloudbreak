package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.ScalingPath;
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
                () -> underTest.validateStackForUpscale(Set.of(), stack, null));
        assertEquals("There are no instances available for scaling!", exception.getMessage());
    }

    @Test
    public void testUpscaleIfMoreInstancesExistsThenValidationFails() {
        Stack stack = mock(Stack.class);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForUpscale(createValidImSet(4), stack, null));
        assertEquals("Upscaling currently only available for FreeIPA installation with 1 or 2 instances", exception.getMessage());
    }

    @Test
    public void testUpscaleIfUnavailableInstanceExistsThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        validImSet.removeIf(instanceMetaData -> instanceMetaData.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY);
        InstanceMetaData pgw = createPrimaryGateway();
        validImSet.add(pgw);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForUpscale(validImSet, stack, null));
        assertEquals("Some of the instances is not available. Please fix them first! Instances: [pgw]", exception.getMessage());
    }

    @Test
    public void testUpscaleIfStackIsUnavailableThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(stack.isAvailable()).thenReturn(false);
        when(stack.getStackStatus()).thenReturn(createStackStatus(false));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForUpscale(validImSet, stack, null));
        assertEquals("Stack is not in available state, refusing to upscale. Current state: UNHEALTHY", exception.getMessage());
    }

    @Test
    public void testUpscaleIfPathIsPermittedThenValidationPasses() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(stack.isAvailable()).thenReturn(true);
        when(allowedScalingPaths.getPaths()).thenReturn(createAllowedScalingPaths());

        assertDoesNotThrow(() -> underTest.validateStackForUpscale(validImSet, stack, createScalingPath(true)));
    }

    @Test
    public void testUpscaleIfPathIsNotPermittedAndNoAlternativeThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(stack.isAvailable()).thenReturn(true);
        when(allowedScalingPaths.getPaths()).thenReturn(createAllowedScalingPaths());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForUpscale(validImSet, stack, createScalingPath(false)));
        assertEquals("Refusing upscale as scaling from 3 node to 1 is not supported.", exception.getMessage());
    }

    @Test
    public void testUpscaleIfPathNotPermittedAndAlternativeExistsThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(stack.isAvailable()).thenReturn(true);
        Map<AvailabilityType, List<AvailabilityType>> allowedScalingPaths = createAllowedScalingPaths();
        allowedScalingPaths.put(AvailabilityType.HA, List.of(AvailabilityType.TWO_NODE_BASED));

        when(this.allowedScalingPaths.getPaths()).thenReturn(allowedScalingPaths);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForUpscale(validImSet, stack, createScalingPath(false)));
        assertEquals("Refusing upscale as scaling from 3 node to 1 is not supported. Supported upscale targets: [TWO_NODE_BASED]", exception.getMessage());
    }

    @Test
    public void testDownscaleIfInvalidInstancesExistsThenValidationFails() {
        Stack stack = mock(Stack.class);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForDownscale(createValidImSet(2), stack, null));
        assertEquals("Downscaling currently only available for FreeIPA installation with 3 instances", exception.getMessage());
    }

    @Test
    public void testDownscaleIfPathIsPermittedThenValidationPasses() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(3);
        when(stack.isAvailable()).thenReturn(true);
        when(allowedScalingPaths.getPaths()).thenReturn(createAllowedScalingPaths());

        assertDoesNotThrow(() -> underTest.validateStackForDownscale(validImSet, stack, createScalingPath(true)));
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

    private Map<AvailabilityType, List<AvailabilityType>> createAllowedScalingPaths() {
        Map<AvailabilityType, List<AvailabilityType>> allowedPaths = new HashMap<>();
        allowedPaths.put(AvailabilityType.NON_HA, List.of(AvailabilityType.HA, AvailabilityType.TWO_NODE_BASED));
        return allowedPaths;
    }

    private ScalingPath createScalingPath(boolean allowed) {
        AvailabilityType originalAvailabilityType = AvailabilityType.NON_HA;
        AvailabilityType targetAvailabilityType = AvailabilityType.HA;
        return allowed
                ? new ScalingPath(originalAvailabilityType, targetAvailabilityType)
                : new ScalingPath(targetAvailabilityType, originalAvailabilityType);
    }
}