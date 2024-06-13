package com.sequenceiq.freeipa.flow.freeipa.rebuild.handler;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.health.RebuildValidateHealthFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.health.RebuildValidateHealthRequest;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.health.RebuildValidateHealthSuccess;
import com.sequenceiq.freeipa.service.stack.FreeIpaSafeInstanceHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class RebuildValidateHealthHandlerTest {
    private static final Long RESOURCE_ID = 1L;

    @Mock
    private FreeIpaSafeInstanceHealthDetailsService healthService;

    @Mock
    private StackService stackService;

    @InjectMocks
    private RebuildValidateHealthHandler handler;

    @Test
    public void testDoAcceptSuccess() {
        RebuildValidateHealthRequest request = new RebuildValidateHealthRequest(RESOURCE_ID);
        HandlerEvent<RebuildValidateHealthRequest> event = new HandlerEvent<>(new Event<>(request));

        Stack stack = mock(Stack.class);
        InstanceMetaData im1 = mock(InstanceMetaData.class);
        InstanceMetaData im2 = mock(InstanceMetaData.class);

        NodeHealthDetails healthDetails1 = new NodeHealthDetails();
        healthDetails1.setStatus(InstanceStatus.CREATED);
        NodeHealthDetails healthDetails2 = new NodeHealthDetails();
        healthDetails2.setStatus(InstanceStatus.CREATED);

        when(stackService.getByIdWithListsInTransaction(RESOURCE_ID)).thenReturn(stack);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(im1, im2));
        when(healthService.getInstanceHealthDetails(stack, im1)).thenReturn(healthDetails1);
        when(healthService.getInstanceHealthDetails(stack, im2)).thenReturn(healthDetails2);

        Selectable result = handler.doAccept(event);

        assertInstanceOf(RebuildValidateHealthSuccess.class, result);
        verify(healthService).getInstanceHealthDetails(stack, im1);
        verify(healthService).getInstanceHealthDetails(stack, im2);
    }

    @Test
    public void testDoAcceptFailure() {
        RebuildValidateHealthRequest request = new RebuildValidateHealthRequest(RESOURCE_ID);
        HandlerEvent<RebuildValidateHealthRequest> event = new HandlerEvent<>(new Event<>(request));

        Stack stack = mock(Stack.class);
        InstanceMetaData im1 = mock(InstanceMetaData.class);
        InstanceMetaData im2 = mock(InstanceMetaData.class);

        NodeHealthDetails healthDetails1 = new NodeHealthDetails();
        healthDetails1.setStatus(InstanceStatus.UNHEALTHY);
        NodeHealthDetails healthDetails2 = new NodeHealthDetails();
        healthDetails2.setStatus(InstanceStatus.CREATED);

        when(stackService.getByIdWithListsInTransaction(RESOURCE_ID)).thenReturn(stack);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(im1, im2));
        when(healthService.getInstanceHealthDetails(stack, im1)).thenReturn(healthDetails1);
        when(healthService.getInstanceHealthDetails(stack, im2)).thenReturn(healthDetails2);

        Selectable result = handler.doAccept(event);

        assertInstanceOf(RebuildValidateHealthFailed.class, result);
        verify(healthService).getInstanceHealthDetails(stack, im1);
        verify(healthService).getInstanceHealthDetails(stack, im2);
    }
}
