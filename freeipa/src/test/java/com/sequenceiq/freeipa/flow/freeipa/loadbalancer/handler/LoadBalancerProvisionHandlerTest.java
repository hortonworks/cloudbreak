package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.handler;

import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerCreationEvent.FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerCreationEvent.PROVISION_FINISHED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.provision.LoadBalancerProvisionRequest;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerCreationService;

@ExtendWith(MockitoExtension.class)
class LoadBalancerProvisionHandlerTest {

    @InjectMocks
    private LoadBalancerProvisionHandler underTest;

    @Mock
    private FreeIpaLoadBalancerCreationService freeIpaLoadBalancerCreationService;

    @Test
    void testHandlerShouldCreateLoadBalancer() {
        LoadBalancerProvisionRequest request = mock(LoadBalancerProvisionRequest.class);

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertEquals(PROVISION_FINISHED_EVENT.event(), result.getSelector());
        verify(freeIpaLoadBalancerCreationService).createLoadBalancer(request);
    }

    @Test
    void testHandlerShouldReturnWithFailureEvent() {
        LoadBalancerProvisionRequest request = mock(LoadBalancerProvisionRequest.class);
        doThrow(new RuntimeException("error")).when(freeIpaLoadBalancerCreationService).createLoadBalancer(request);

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertEquals(FAILURE_EVENT.event(), result.getSelector());
        verify(freeIpaLoadBalancerCreationService).createLoadBalancer(request);
    }

}