package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.handler;

import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerCreationEvent.FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerCreationEvent.METADATA_COLLECTION_FINISHED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.metadata.LoadBalancerMetadataCollectionRequest;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerMetadataCollectionService;

@ExtendWith(MockitoExtension.class)
class LoadBalancerMetadataCollectionHandlerTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private LoadBalancerMetadataCollectionHandler underTest;

    @Mock
    private FreeIpaLoadBalancerMetadataCollectionService freeIpaLoadBalancerMetadataCollectionService;

    @Test
    void testHandlerShouldReturnWithSuccessEvent() {
        LoadBalancerMetadataCollectionRequest request = new LoadBalancerMetadataCollectionRequest(STACK_ID, null, null, null);

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertEquals(METADATA_COLLECTION_FINISHED_EVENT.event(), result.selector());
        verify(freeIpaLoadBalancerMetadataCollectionService).collectLoadBalancerMetadata(request);
    }

    @Test
    void testHandlerShouldReturnWithFailureEvent() {
        LoadBalancerMetadataCollectionRequest request = new LoadBalancerMetadataCollectionRequest(STACK_ID, null, null, null);
        doThrow(new RuntimeException("error")).when(freeIpaLoadBalancerMetadataCollectionService).collectLoadBalancerMetadata(request);

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertEquals(FAILURE_EVENT.event(), result.selector());
        verify(freeIpaLoadBalancerMetadataCollectionService).collectLoadBalancerMetadata(request);
    }
}