package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerUpdateFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerUpdateRequest;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerUpdateSuccess;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerUpdateService;

@ExtendWith(MockitoExtension.class)
class LoadBalancerUpdateHandlerTest {

    @InjectMocks
    private LoadBalancerUpdateHandler underTest;

    @Mock
    private FreeIpaLoadBalancerUpdateService loadBalancerUpdateService;

    @Mock
    private FreeIpaLoadBalancerService loadBalancerService;

    @Test
    void testHandlerShouldUpdateLoadBalancer() {
        LoadBalancerUpdateRequest request = mock(LoadBalancerUpdateRequest.class);
        when(request.getResourceId()).thenReturn(1L);
        when(loadBalancerService.findByStackId(1L)).thenReturn(Optional.of(new LoadBalancer()));

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertEquals(EventSelectorUtil.selector(LoadBalancerUpdateSuccess.class), result.getSelector());
        verify(loadBalancerUpdateService).updateLoadBalancer(request);
    }

    @Test
    void testHandlerShouldSkipUpdateIfLoadBalancerMissing() {
        LoadBalancerUpdateRequest request = mock(LoadBalancerUpdateRequest.class);
        when(request.getResourceId()).thenReturn(1L);
        when(loadBalancerService.findByStackId(1L)).thenReturn(Optional.empty());

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertEquals(EventSelectorUtil.selector(LoadBalancerUpdateSuccess.class), result.getSelector());
        verifyNoInteractions(loadBalancerUpdateService);
    }

    @Test
    void testHandlerShouldReturnWithFailureEvent() {
        LoadBalancerUpdateRequest request = mock(LoadBalancerUpdateRequest.class);
        when(request.getResourceId()).thenReturn(1L);
        when(loadBalancerService.findByStackId(1L)).thenReturn(Optional.of(new LoadBalancer()));
        doThrow(new RuntimeException("error")).when(loadBalancerUpdateService).updateLoadBalancer(request);

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertEquals(EventSelectorUtil.selector(LoadBalancerUpdateFailureEvent.class), result.getSelector());
        verify(loadBalancerUpdateService).updateLoadBalancer(request);
    }

}