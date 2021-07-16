package com.sequenceiq.freeipa.flow.stack.provision.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationSuccess;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class ClusterProxyRegistrationHandlerTest {

    @Mock
    private EventBus eventBus;

    @Mock
    private ClusterProxyService clusterProxyService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private ClusterProxyRegistrationHandler underTest;

    @Test
    public void testSelector() {
        assertEquals(EventSelectorUtil.selector(ClusterProxyRegistrationRequest.class), underTest.selector());
    }

    @Test
    public void testAllInstanceHaveFqdn() {
        when(instanceMetaDataService.findNotTerminatedForStack(1L)).thenReturn(Set.of(createInstanceMetadata("aa"), createInstanceMetadata("bb")));

        underTest.accept(new Event<>(new ClusterProxyRegistrationRequest(1L)));

        verify(clusterProxyService).registerFreeIpa(1L);
        verifyNoMoreInteractions(clusterProxyService);
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        String selector = selectorCaptor.getValue();
        assertEquals(EventSelectorUtil.selector(ClusterProxyRegistrationSuccess.class), selector);
        validateResourceId(eventCaptor);
    }

    private void validateResourceId(ArgumentCaptor<Event> eventCaptor) {
        ClusterProxyRegistrationSuccess event = (ClusterProxyRegistrationSuccess) eventCaptor.getValue().getData();
        assertEquals(1L, event.getResourceId());
    }

    @Test
    public void testNotAllInstanceHaveFqdn() {
        when(instanceMetaDataService.findNotTerminatedForStack(1L)).thenReturn(Set.of(createInstanceMetadata("aa"), createInstanceMetadata(null)));

        underTest.accept(new Event<>(new ClusterProxyRegistrationRequest(1L)));

        verify(clusterProxyService).registerFreeIpaForBootstrap(1L);
        verifyNoMoreInteractions(clusterProxyService);
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        String selector = selectorCaptor.getValue();
        assertEquals(EventSelectorUtil.selector(ClusterProxyRegistrationSuccess.class), selector);
        validateResourceId(eventCaptor);
    }

    @Test
    public void testRegistrationFails() {
        when(instanceMetaDataService.findNotTerminatedForStack(1L)).thenReturn(Set.of(createInstanceMetadata("aa"), createInstanceMetadata(null)));
        when(clusterProxyService.registerFreeIpaForBootstrap(1L)).thenThrow(new RuntimeException("bumm"));

        underTest.accept(new Event<>(new ClusterProxyRegistrationRequest(1L)));

        verifyNoMoreInteractions(clusterProxyService);
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        String selector = selectorCaptor.getValue();
        assertEquals(EventSelectorUtil.selector(ClusterProxyRegistrationFailed.class), selector);
        ClusterProxyRegistrationFailed event = (ClusterProxyRegistrationFailed) eventCaptor.getValue().getData();
        assertEquals(1L, event.getResourceId());
    }

    @Test
    public void testRegistrationFailsWithFqdnFilled() {
        when(instanceMetaDataService.findNotTerminatedForStack(1L)).thenReturn(Set.of(createInstanceMetadata("aa"), createInstanceMetadata("test")));
        when(clusterProxyService.registerFreeIpa(1L)).thenThrow(new RuntimeException("bumm"));

        underTest.accept(new Event<>(new ClusterProxyRegistrationRequest(1L)));

        verifyNoMoreInteractions(clusterProxyService);
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        String selector = selectorCaptor.getValue();
        assertEquals(EventSelectorUtil.selector(ClusterProxyRegistrationFailed.class), selector);
        ClusterProxyRegistrationFailed event = (ClusterProxyRegistrationFailed) eventCaptor.getValue().getData();
        assertEquals(1L, event.getResourceId());
    }

    @Test
    public void testRegistrationFailsWithNoInstancesFound() {
        when(instanceMetaDataService.findNotTerminatedForStack(1L)).thenReturn(Set.of());

        underTest.accept(new Event<>(new ClusterProxyRegistrationRequest(1L)));

        verifyNoInteractions(clusterProxyService);
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(selectorCaptor.capture(), eventCaptor.capture());
        String selector = selectorCaptor.getValue();
        assertEquals(EventSelectorUtil.selector(ClusterProxyRegistrationFailed.class), selector);
        ClusterProxyRegistrationFailed event = (ClusterProxyRegistrationFailed) eventCaptor.getValue().getData();
        assertEquals(1L, event.getResourceId());
    }

    private InstanceMetaData createInstanceMetadata(String fqdn) {
        InstanceMetaData im = new InstanceMetaData();
        im.setDiscoveryFQDN(fqdn);
        return im;
    }
}