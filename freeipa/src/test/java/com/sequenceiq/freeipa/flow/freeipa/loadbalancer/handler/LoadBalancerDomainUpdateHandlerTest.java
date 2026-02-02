package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.PemDnsEntryCreateOrUpdateException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerCreationFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerDomainUpdateRequest;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerDomainUpdateSuccess;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerDomainService;

@ExtendWith(MockitoExtension.class)
class LoadBalancerDomainUpdateHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private FreeIpaLoadBalancerDomainService loadBalancerDomainService;

    @InjectMocks
    private LoadBalancerDomainUpdateHandler underTest;

    private HandlerEvent<LoadBalancerDomainUpdateRequest> handlerEvent;

    @BeforeEach
    void setUp() {
        LoadBalancerDomainUpdateRequest request = new LoadBalancerDomainUpdateRequest(STACK_ID);
        handlerEvent = new HandlerEvent<>(new Event<>(request));
    }

    @Test
    void selector() {
        assertEquals(EventSelectorUtil.selector(LoadBalancerDomainUpdateRequest.class), underTest.selector());
    }

    @Test
    void doAcceptSuccess() throws Exception {
        Selectable result = underTest.doAccept(handlerEvent);
        verify(loadBalancerDomainService).registerLbDomain(STACK_ID);
        assertEquals(LoadBalancerDomainUpdateSuccess.class, result.getClass());
    }

    @Test
    void doAcceptFreeIpaClientException() throws Exception {
        doThrow(new FreeIpaClientException("failure")).when(loadBalancerDomainService).registerLbDomain(STACK_ID);
        Selectable result = underTest.doAccept(handlerEvent);
        assertEquals(LoadBalancerCreationFailureEvent.class, result.getClass());
    }

    @Test
    void doAcceptPemDnsEntryCreateOrUpdateException() throws Exception {
        doThrow(new PemDnsEntryCreateOrUpdateException("failure")).when(loadBalancerDomainService).registerLbDomain(STACK_ID);
        Selectable result = underTest.doAccept(handlerEvent);
        assertEquals(LoadBalancerCreationFailureEvent.class, result.getClass());
    }

    @Test
    void defaultFailureEvent() {
        Exception e = new Exception("failure");
        Selectable result = underTest.defaultFailureEvent(STACK_ID, e, handlerEvent.getEvent());
        assertEquals(LoadBalancerCreationFailureEvent.class, result.getClass());
        LoadBalancerCreationFailureEvent failureEvent = (LoadBalancerCreationFailureEvent) result;
        assertEquals(FailureType.ERROR, failureEvent.getFailureType());
        assertEquals(e, failureEvent.getException());
    }
}
