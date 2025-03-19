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
    void testUpscaleIfNoInstanceExistsThenValidationFails() {
        Stack stack = mock(Stack.class);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForUpscale(Set.of(), stack, new ScalingPath(NON_HA, HA)));
        assertEquals("There are no instances available for scaling!", exception.getMessage());
    }

    @Test
    void testDownscaleIfNoInstanceExistsThenValidationFails() {
        Stack stack = mock(Stack.class);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForDownscale(Set.of(), stack, new ScalingPath(HA, NON_HA), null, false));
        assertEquals("There are no instances available for scaling!", exception.getMessage());
    }

    @Test
    void testUpscaleIfUnavailableInstanceExistsThenValidationFails() {
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
    void testDownscaleIfUnavailableInstanceExistsThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        validImSet.removeIf(instanceMetaData -> instanceMetaData.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY);
        InstanceMetaData pgw = createPrimaryGateway();
        validImSet.add(pgw);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, NON_HA), null, false));
        assertEquals("Some of the instances is not available. Please fix them first! Instances: [pgw]", exception.getMessage());
    }

    @Test
    void testDownscaleIfUnavailableInstanceExistsThenValidationPassWhenForce() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(3);
        validImSet.removeIf(instanceMetaData -> instanceMetaData.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY);
        InstanceMetaData pgw = createPrimaryGateway();
        validImSet.add(pgw);
        when(this.allowedScalingPaths.getPaths()).thenReturn(Map.of(HA, List.of(TWO_NODE_BASED)));

        underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, TWO_NODE_BASED), null, true);
    }

    @Test
    void testUpscaleIfStackIsUnavailableThenValidationFails() {
        Stack stack = getStack(false);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(stack.getStackStatus()).thenReturn(createStackStatus(false));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForUpscale(validImSet, stack, new ScalingPath(NON_HA, HA)));
        assertEquals("Stack is not in available state, refusing to upscale. Current state: UNHEALTHY", exception.getMessage());
    }

    @Test
    void testDownscaleIfStackIsUnavailableThenValidationFails() {
        Stack stack = getStack(false);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(stack.getStackStatus()).thenReturn(createStackStatus(false));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, TWO_NODE_BASED), null, false));
        assertEquals("Stack is not in available state, refusing to downscale. Current state: UNHEALTHY", exception.getMessage());
    }

    @Test
    void testDownscaleIfStackIsUnavailableThenValidationPassWhenForce() {
        Stack stack = getStack(false);
        Set<InstanceMetaData> validImSet = createValidImSet(3);
        when(this.allowedScalingPaths.getPaths()).thenReturn(Map.of(HA, List.of(TWO_NODE_BASED)));

        underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, TWO_NODE_BASED), null, true);
    }

    @Test
    void testUpscaleIfPathIsPermittedThenValidationPasses() {
        Stack stack = getStack(true);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(allowedScalingPaths.getPaths()).thenReturn(Map.of(NON_HA, List.of(HA)));

        assertDoesNotThrow(() -> underTest.validateStackForUpscale(validImSet, stack, new ScalingPath(NON_HA, HA)));
    }

    @Test
    void testDownscaleIfPathIsPermittedThenValidationPasses() {
        Stack stack = getStack(true);
        Set<InstanceMetaData> validImSet = createValidImSet(3);
        when(allowedScalingPaths.getPaths()).thenReturn(Map.of(HA, List.of(TWO_NODE_BASED)));

        assertDoesNotThrow(() -> underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, TWO_NODE_BASED), null, false));
    }

    @Test
    void testUpscaleIfTargetNodeCountSmallerThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(3);

        assertThatCode(() -> underTest.validateStackForUpscale(validImSet, stack, new ScalingPath(HA, TWO_NODE_BASED)))
                .isExactlyInstanceOf(BadRequestException.class)
                .hasMessage("Refusing upscale as target node count is smaller than current. Current node count: 3, target node count: 2.");
    }

    @Test
    void testDownscaleIfTargetNodeCountHigherThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);

        assertThatCode(() -> underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(TWO_NODE_BASED, HA), null, false))
                .isExactlyInstanceOf(BadRequestException.class)
                .hasMessage("Refusing downscale as target node count is higher than current. Current node count: 2, target node count: 3.");
    }

    @Test
    void testUpscaleIfPathIsNotPermittedAndNoAlternativeThenValidationFails() {
        Stack stack = getStack(true);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(allowedScalingPaths.getPaths()).thenReturn(Map.of());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForUpscale(validImSet, stack, new ScalingPath(TWO_NODE_BASED, HA)));
        assertEquals("Refusing upscale as scaling from 2 node to 3 is not supported.", exception.getMessage());
    }

    @Test
    void testDownscaleIfPathIsNotPermittedAndNoAlternativeThenValidationFails() {
        Stack stack = getStack(true);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(allowedScalingPaths.getPaths()).thenReturn(Map.of());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, NON_HA), null, false));
        assertEquals("Refusing downscale as scaling from 3 node to 1 is not supported.", exception.getMessage());
    }

    @Test
    void testUpscaleIfPathNotPermittedAndAlternativeExistsThenValidationFails() {
        Stack stack = getStack(true);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(this.allowedScalingPaths.getPaths()).thenReturn(Map.of(NON_HA, List.of(TWO_NODE_BASED)));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForUpscale(validImSet, stack, new ScalingPath(NON_HA, HA)));
        assertEquals("Refusing upscale as scaling from 1 node to 3 is not supported. Supported upscale targets: [TWO_NODE_BASED]", exception.getMessage());
    }

    @Test
    void testDownscaleIfPathNotPermittedAndAlternativeExistsThenValidationFails() {
        Stack stack = getStack(true);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        when(this.allowedScalingPaths.getPaths()).thenReturn(Map.of(HA, List.of(TWO_NODE_BASED)));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, NON_HA), null, false));
        assertEquals("Refusing downscale as scaling from 3 node to 1 is not supported. Supported downscale targets: [TWO_NODE_BASED]", exception.getMessage());
    }

    @Test
    void testUpscaleIfNodeCountWouldNotChangeThenValidationFails() {
        Stack stack = getStack(true);
        Set<InstanceMetaData> validImSet = createValidImSet(2);

        assertThatCode(() -> underTest.validateStackForUpscale(validImSet, stack, new ScalingPath(TWO_NODE_BASED, TWO_NODE_BASED)))
                .isExactlyInstanceOf(BadRequestException.class)
                .hasMessage("Refusing UPSCALE as the current node count already matches the node count of the requested availability type. Current " +
                        "node count: 2, target availability type: TWO_NODE_BASED and node count: 2.");
    }

    @ParameterizedTest
    @EnumSource(value = AvailabilityType.class)
    void testDownscaleIfNodeCountWouldNotChangeThenValidationFails(AvailabilityType availabilityType) {
        Stack stack = getStack(true);
        Set<InstanceMetaData> validImSet = createValidImSet(availabilityType.getInstanceCount());

        assertThatCode(() -> underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(availabilityType, availabilityType), null, false))
                .isExactlyInstanceOf(BadRequestException.class)
                .hasMessage(String.format("Refusing DOWNSCALE as the current node count already matches the node count of the requested availability type. " +
                        "Current node count: %d, target availability type: %s and node count: %d.",
                        availabilityType.getInstanceCount(), availabilityType.name(), availabilityType.getInstanceCount()));
    }

    @Test
    void testDownscaleIfInstanceIdsEmptyThenValidationPasses() {
        Stack stack = getStack(true);
        Set<InstanceMetaData> validImSet = createValidImSet(3);
        when(this.allowedScalingPaths.getPaths()).thenReturn(Map.of(HA, List.of(TWO_NODE_BASED)));

        underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, TWO_NODE_BASED), null, false);
    }

    @Test
    void testDownscaleIfInstanceIdsAmongAllNodesThenValidationPasses() {
        Stack stack = getStack(true);
        Set<InstanceMetaData> validImSet = createValidImSet(3);
        when(this.allowedScalingPaths.getPaths()).thenReturn(Map.of(HA, List.of(TWO_NODE_BASED)));

        underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, TWO_NODE_BASED), Set.of("im2"), false);
    }

    @Test
    void testValidateStackForDownscaleForMultiAzSuccess() {
        Stack stack = getStack(true);
        Set<InstanceMetaData> validImSet = createValidImSet(3, true);
        when(this.allowedScalingPaths.getPaths()).thenReturn(Map.of(HA, List.of(TWO_NODE_BASED, NON_HA)));
        underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, NON_HA), Set.of("im2"), false);
    }

    @Test
    void testValidateStackForDownscaleForMultiAzWithValidationsSkipped() {
        Stack stack = getStack(true);
        Set<InstanceMetaData> validImSet = createValidImSet(3, true);
        when(this.allowedScalingPaths.getPaths()).thenReturn(Map.of(HA, List.of(TWO_NODE_BASED, NON_HA)));
        underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, NON_HA), Set.of("im2", "im3"), false);
    }

    private Stack getStack(boolean available) {
        Stack stack = mock(Stack.class);
        when(stack.isAvailable()).thenReturn(available);
        return stack;
    }

    @Test
    void testDownscaleIfInstanceIdsNotAmongAllNodesThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(3);

        assertThatCode(() -> underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, TWO_NODE_BASED), Set.of("unknownNode"), false))
                .isExactlyInstanceOf(BadRequestException.class)
                .hasMessage("Refusing downscale as some of the selected instance ids are not part of the cluster. Unknown instance ids: [unknownNode].");
    }

    @Test
    void testDownscaleIfInstanceIdToDeleteContainsPrimaryGatewayThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(3);

        assertThatCode(() -> underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, TWO_NODE_BASED), Set.of("pgw"), false))
                .isExactlyInstanceOf(BadRequestException.class)
                .hasMessage("Refusing downscale as instance ids contains an instance that is a primary gateway. Please select another instance. Primary " +
                        "gateway instance: [pgw].");

    }

    @Test
    void testDownscaleIfInstanceIdToDeleteContainsPrimaryGatewayThenValidationDoensFailsWhenForced() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(3);
        when(this.allowedScalingPaths.getPaths()).thenReturn(Map.of(HA, List.of(TWO_NODE_BASED)));

        underTest.validateStackForDownscale(validImSet, stack, new ScalingPath(HA, TWO_NODE_BASED), Set.of("pgw"), true);
    }

    private InstanceMetaData createPrimaryGateway() {
        InstanceMetaData pgw = new InstanceMetaData();
        pgw.setInstanceStatus(InstanceStatus.UNREACHABLE);
        pgw.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        pgw.setInstanceId("pgw");
        return pgw;
    }

    private Set<InstanceMetaData> createValidImSet(int instanceCount) {
        return createValidImSet(instanceCount, false);
    }

    private Set<InstanceMetaData> createValidImSet(int instanceCount, boolean multiAz) {
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
            if (multiAz) {
                im.setAvailabilityZone(String.valueOf(i));
            }
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