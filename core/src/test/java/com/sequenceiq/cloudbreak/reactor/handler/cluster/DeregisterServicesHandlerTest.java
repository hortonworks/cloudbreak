package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DeregisterServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DeregisterServicesResult;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class DeregisterServicesHandlerTest {

    private static final Long TEST_STACK_ID = 1L;

    @Mock
    private EventBus eventBus;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Mock
    private StackDtoService stackDtoService;

    @InjectMocks
    private DeregisterServicesHandler underTest;

    private Event<DeregisterServicesRequest> event = new Event<>(new DeregisterServicesRequest(TEST_STACK_ID, false));

    private Event<DeregisterServicesRequest> eventForced = new Event<>(new DeregisterServicesRequest(TEST_STACK_ID, true));

    @Test
    void testAcceptEvent() {
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(anyString())).thenReturn(Optional.of(SdxBasicView.builder().withCrn("sdxCrn").build()));
        StackView stack = mock(StackView.class);
        when(stackDtoService.getStackViewById(any())).thenReturn(stack);
        when(stack.getResourceCrn()).thenReturn("dhCrn");
        when(stack.getEnvironmentCrn()).thenReturn("envCrn");
        doNothing().when(platformAwareSdxConnector).tearDownDatahub(any(), any());

        underTest.accept(event);

        verify(platformAwareSdxConnector).tearDownDatahub(any(), any());
    }

    @Test
    void testAcceptEventWhenNoSdx() {
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(anyString())).thenReturn(Optional.empty());
        StackView stack = mock(StackView.class);
        when(stackDtoService.getStackViewById(any())).thenReturn(stack);
        when(stack.getEnvironmentCrn()).thenReturn("envCrn");

        underTest.accept(event);

        verify(platformAwareSdxConnector, never()).tearDownDatahub(any(), any());
    }

    @Test
    void testAcceptEventThrowsException() {
        when(stackDtoService.getStackViewById(anyLong())).thenReturn(new Stack());
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenThrow(new RuntimeException("the message"));

        underTest.accept(event);

        ArgumentCaptor<Event<DeregisterServicesResult>> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(any(), argumentCaptor.capture());
        Event resultEvent = argumentCaptor.getValue();
        DeregisterServicesResult result = (DeregisterServicesResult) resultEvent.getData();
        assertNotNull(result.getErrorDetails());
        assertNotNull(result.getStatusReason());
        assertEquals(EventStatus.FAILED, result.getStatus());
    }

    @Test
    void testResultWhenExceptionThrownAndForcedFlag() {
        when(stackDtoService.getStackViewById(anyLong())).thenReturn(new Stack());
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenThrow(new RuntimeException("the message"));

        underTest.accept(eventForced);

        ArgumentCaptor<Event<DeregisterServicesResult>> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(any(), argumentCaptor.capture());
        Event resultEvent = argumentCaptor.getValue();
        DeregisterServicesResult result = (DeregisterServicesResult) resultEvent.getData();
        assertNull(result.getException());
        assertNull(result.getErrorDetails());
        assertEquals(EventStatus.OK, result.getStatus());
    }
}