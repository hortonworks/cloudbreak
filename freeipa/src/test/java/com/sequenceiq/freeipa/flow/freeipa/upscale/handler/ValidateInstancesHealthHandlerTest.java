package com.sequenceiq.freeipa.flow.freeipa.upscale.handler;

import static com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent.UPSCALE_VALIDATE_NEW_INSTANCES_HEALTH_FINISHED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.ValidateInstancesHealthEvent;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.stack.FreeIpaInstanceHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class ValidateInstancesHealthHandlerTest {

    private static final String PHASE = "Instance health validation";

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private FreeIpaInstanceHealthDetailsService healthService;

    @Mock
    private StackService stackService;

    @InjectMocks
    private ValidateInstancesHealthHandler underTest;

    @Test
    public void testSelector() {
        assertEquals(EventSelectorUtil.selector(ValidateInstancesHealthEvent.class), underTest.selector());
    }

    @Test
    public void testDefaultFailureEvent() {
        Exception e = new Exception("puff");
        Event<ValidateInstancesHealthEvent> event = new Event<>(new ValidateInstancesHealthEvent(3L, List.of()));

        UpscaleFailureEvent result = (UpscaleFailureEvent) underTest.defaultFailureEvent(2L, e, event);

        assertEquals(e, result.getException());
        assertEquals(2L, result.getResourceId());
        assertEquals(PHASE, result.getFailedPhase());
    }

    @Test
    public void testHealthy() throws FreeIpaClientException {
        Stack stack = new Stack();
        stack.setId(1L);
        when(stackService.getStackById(1L)).thenReturn(stack);
        List<String> instanceIds = List.of("im1", "im2");
        InstanceMetaData im1 = new InstanceMetaData();
        im1.setInstanceId("im1");
        im1.setDiscoveryFQDN("im1Fqdn");
        InstanceMetaData im2 = new InstanceMetaData();
        im2.setInstanceId("im2");
        im2.setDiscoveryFQDN("im2Fqdn");
        when(instanceMetaDataService.getNotTerminatedByInstanceIds(1L, instanceIds)).thenReturn(Set.of(im1, im2));
        when(healthService.getInstanceHealthDetails(stack, im1)).thenReturn(createHealthyNodeDetail(im1));
        when(healthService.getInstanceHealthDetails(stack, im2)).thenReturn(createHealthyNodeDetail(im2));

        StackEvent result = (StackEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(new ValidateInstancesHealthEvent(1L, instanceIds))));

        assertEquals(UPSCALE_VALIDATE_NEW_INSTANCES_HEALTH_FINISHED_EVENT.event(), result.selector());
        assertEquals(1L, result.getResourceId());
    }

    @Test
    public void testUnHealthy() throws FreeIpaClientException {
        Stack stack = new Stack();
        stack.setId(1L);
        when(stackService.getStackById(1L)).thenReturn(stack);
        List<String> instanceIds = List.of("im1", "im2");
        InstanceMetaData im1 = new InstanceMetaData();
        im1.setInstanceId("im1");
        im1.setDiscoveryFQDN("im1Fqdn");
        InstanceMetaData im2 = new InstanceMetaData();
        im2.setInstanceId("im2");
        im2.setDiscoveryFQDN("im2Fqdn");
        when(instanceMetaDataService.getNotTerminatedByInstanceIds(1L, instanceIds)).thenReturn(Set.of(im1, im2));
        when(healthService.getInstanceHealthDetails(stack, im1)).thenReturn(createHealthyNodeDetail(im1));
        NodeHealthDetails unhealthy = createHealthyNodeDetail(im2);
        unhealthy.setStatus(InstanceStatus.UNHEALTHY);
        unhealthy.setIssues(List.of("bad"));
        when(healthService.getInstanceHealthDetails(stack, im2)).thenReturn(unhealthy);

        ValidateInstancesHealthEvent validateInstancesHealthEvent = new ValidateInstancesHealthEvent(1L, instanceIds);
        UpscaleFailureEvent result = (UpscaleFailureEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(validateInstancesHealthEvent)));

        assertEquals(1L, result.getResourceId());
        assertEquals(PHASE, result.getFailedPhase());
        assertTrue(result.getSuccess().contains("im1"));
        assertEquals(1, result.getFailureDetails().size());
        assertEquals("bad", result.getFailureDetails().get("im2"));
        assertEquals("Unhealthy instances found: [im2]", result.getException().getMessage());
    }

    @Test
    public void testExceptionDuringHealthCheck() throws FreeIpaClientException {
        Stack stack = new Stack();
        stack.setId(1L);
        when(stackService.getStackById(1L)).thenReturn(stack);
        List<String> instanceIds = List.of("im1", "im2");
        InstanceMetaData im1 = new InstanceMetaData();
        im1.setInstanceId("im1");
        im1.setDiscoveryFQDN("im1Fqdn");
        InstanceMetaData im2 = new InstanceMetaData();
        im2.setInstanceId("im2");
        im2.setDiscoveryFQDN("im2Fqdn");
        when(instanceMetaDataService.getNotTerminatedByInstanceIds(1L, instanceIds)).thenReturn(Set.of(im1, im2));
        when(healthService.getInstanceHealthDetails(stack, im1)).thenReturn(createHealthyNodeDetail(im1));
        when(healthService.getInstanceHealthDetails(stack, im2)).thenThrow(new FreeIpaClientException("nono"));

        ValidateInstancesHealthEvent validateInstancesHealthEvent = new ValidateInstancesHealthEvent(1L, instanceIds);
        UpscaleFailureEvent result = (UpscaleFailureEvent) underTest.doAccept(new HandlerEvent<>(new Event<>(validateInstancesHealthEvent)));

        assertEquals(1L, result.getResourceId());
        assertEquals(PHASE, result.getFailedPhase());
        assertTrue(result.getSuccess().contains("im1"));
        assertEquals(1, result.getFailureDetails().size());
        assertEquals("nono", result.getFailureDetails().get("im2"));
        assertEquals("Unhealthy instances found: [im2]", result.getException().getMessage());
    }

    private NodeHealthDetails createHealthyNodeDetail(InstanceMetaData im) {
        NodeHealthDetails nodeHealthDetails = new NodeHealthDetails();
        nodeHealthDetails.setName(im.getDiscoveryFQDN());
        nodeHealthDetails.setStatus(InstanceStatus.CREATED);
        nodeHealthDetails.setInstanceId(im.getInstanceId());
        return nodeHealthDetails;
    }
}