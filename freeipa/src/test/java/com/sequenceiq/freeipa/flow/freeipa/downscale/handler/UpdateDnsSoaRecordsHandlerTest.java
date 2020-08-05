package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.model.DnsZone;
import com.sequenceiq.freeipa.client.model.IpaServer;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.dnssoarecords.UpdateDnsSoaRecordsRequest;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class UpdateDnsSoaRecordsHandlerTest {

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private UpdateDnsSoaRecordsHandler underTest;

    @Test
    void testResultContainsServerFqdns() throws Exception {
        String zoneName = "example.com.";
        String fqdn1 = "foo1.example.com";
        String fqdn2 = "foo2.example.com";
        FreeIpaClient mockIpaClient = mock(FreeIpaClient.class);
        IpaServer mockIpaServer1 = mock(IpaServer.class);
        IpaServer mockIpaServer2 = mock(IpaServer.class);
        DnsZone dnsZone = mock(DnsZone.class);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(any())).thenReturn(mockIpaClient);
        when(mockIpaClient.findAllServers()).thenReturn(Set.of(mockIpaServer1, mockIpaServer2));
        Mockito.lenient().when(mockIpaServer1.getFqdn()).thenReturn(fqdn1);
        when(mockIpaServer2.getFqdn()).thenReturn(fqdn2);
        when(mockIpaClient.findAllDnsZone()).thenReturn(Set.of(dnsZone));
        when(dnsZone.getIdnssoamname()).thenReturn(fqdn1 + ".");
        when(dnsZone.getIdnsname()).thenReturn(zoneName);


        CleanupEvent cleanupEvent = new CleanupEvent(1L, Set.of(), Set.of(fqdn1), Set.of(), Set.of(), Set.of(), "", "", "", "");
        UpdateDnsSoaRecordsRequest request = new UpdateDnsSoaRecordsRequest(cleanupEvent);

        underTest.accept(new Event<>(request));

        verify(eventBus).notify(eq("UPDATEDNSSOARECORDSRESPONSE"), any(Event.class));
        verify(mockIpaClient).setDnsZoneAuthoritativeNameserver(eq(zoneName), eq(fqdn2 + "."));
    }
}