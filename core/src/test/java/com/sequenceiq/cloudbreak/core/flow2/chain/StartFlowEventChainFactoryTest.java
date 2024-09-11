package com.sequenceiq.cloudbreak.core.flow2.chain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResource;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.RotationEvent;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.MultiClusterRotationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@ExtendWith(MockitoExtension.class)
public class StartFlowEventChainFactoryTest {

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private MultiClusterRotationService multiClusterRotationService;

    @InjectMocks
    private StartFlowEventChainFactory underTest;

    @Test
    void testGetChainIfRotationNeeded() {
        StackDto stack = mock(StackDto.class);
        when(stack.getResourceCrn()).thenReturn("crn");
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        when(stackDtoService.getById(any())).thenReturn(stack);
        MultiClusterRotationResource multiClusterRotationResource = new MultiClusterRotationResource();
        multiClusterRotationResource.setSecretType(MultiSecretType.CM_SERVICE_SHARED_DB);
        when(multiClusterRotationService.getMultiRotationChildEntriesForResource(any())).thenReturn(Set.of(multiClusterRotationResource));

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(new StackEvent(1L));

        assertEquals(4, flowTriggerEventQueue.getQueue().size());
        Optional<SecretType> optionalSecretType = getOptionalSecretType(flowTriggerEventQueue);
        assertTrue(optionalSecretType.isPresent());
        assertEquals(CloudbreakSecretType.CM_SERVICE_SHARED_DB, optionalSecretType.get());
    }

    @Test
    void testGetChainIfRotationNotNeeded() {
        StackDto stack = mock(StackDto.class);
        when(stack.getResourceCrn()).thenReturn("crn");
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        when(stackDtoService.getById(any())).thenReturn(stack);
        when(multiClusterRotationService.getMultiRotationChildEntriesForResource(any())).thenReturn(Set.of());

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(new StackEvent(1L));

        assertEquals(3, flowTriggerEventQueue.getQueue().size());
        Optional<SecretType> optionalSecretType = getOptionalSecretType(flowTriggerEventQueue);
        assertFalse(optionalSecretType.isPresent());
    }

    @Test
    void testGetChainIfRotationNeededButFailureHappens() {
        StackDto stack = mock(StackDto.class);
        when(stack.getResourceCrn()).thenReturn("crn");
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        when(stackDtoService.getById(any())).thenReturn(stack);
        when(multiClusterRotationService.getMultiRotationChildEntriesForResource(any())).thenThrow(new RuntimeException("ops"));

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(new StackEvent(1L));

        assertEquals(3, flowTriggerEventQueue.getQueue().size());
        Optional<SecretType> optionalSecretType = getOptionalSecretType(flowTriggerEventQueue);
        assertFalse(optionalSecretType.isPresent());
    }

    @Test
    void testGetChainRotationNeededIfStackIsDatalake() {
        StackDto stack = mock(StackDto.class);
        when(stack.getType()).thenReturn(StackType.DATALAKE);
        when(stackDtoService.getById(any())).thenReturn(stack);

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(new StackEvent(1L));

        assertEquals(3, flowTriggerEventQueue.getQueue().size());
        Optional<SecretType> optionalSecretType = getOptionalSecretType(flowTriggerEventQueue);
        assertFalse(optionalSecretType.isPresent());
    }

    private static Optional<SecretType> getOptionalSecretType(FlowTriggerEventQueue flowTriggerEventQueue) {
        return flowTriggerEventQueue.getQueue().stream()
                .filter(event -> RotationEvent.class.isAssignableFrom(event.getClass()))
                .map(rotationTriggerEvent -> ((RotationEvent) rotationTriggerEvent).getSecretType())
                .findFirst();
    }
}
