package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.model.IpaServer;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.collecthostnames.CollectAdditionalHostnamesRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.collecthostnames.CollectAdditionalHostnamesResponse;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.bus.Event;
import reactor.bus.EventBus;

import java.util.Set;

@ExtendWith(MockitoExtension.class)
class CollectAdditionalHostnamesHandlerTest {

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private CollectAdditionalHostnamesHandler underTest;

    @Test
    void testResultContainsServerFqdns() throws Exception {
        String fqdn1 = "foo1.example.com";
        String fqdn2 = "foo2.example.com";
        FreeIpaClient mockIpaClient = Mockito.mock(FreeIpaClient.class);
        IpaServer mockIpaServer1 = Mockito.mock(IpaServer.class);
        IpaServer mockIpaServer2 = Mockito.mock(IpaServer.class);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(any())).thenReturn(mockIpaClient);
        when(mockIpaClient.findAllServers()).thenReturn(Set.of(mockIpaServer1, mockIpaServer2));
        when(mockIpaServer1.getFqdn()).thenReturn(fqdn1);
        when(mockIpaServer2.getFqdn()).thenReturn(fqdn2);


        CollectAdditionalHostnamesRequest request = new CollectAdditionalHostnamesRequest(1L);

        underTest.accept(new Event<>(request));

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq("COLLECTADDITIONALHOSTNAMESRESPONSE"), eventCaptor.capture());

        Event event = eventCaptor.getValue();
        assertTrue(event.getData() instanceof CollectAdditionalHostnamesResponse);
        CollectAdditionalHostnamesResponse response = (CollectAdditionalHostnamesResponse) event.getData();
        assertEquals(Set.of(fqdn1, fqdn2), response.getHostnames());
    }
}