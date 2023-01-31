package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.dnssoarecords.UpdateDnsSoaRecordsRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.dnssoarecords.UpdateDnsSoaRecordsResponse;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsSoaRecordService;

@ExtendWith(MockitoExtension.class)
class UpdateDnsSoaRecordsHandlerTest {

    @Mock
    private DnsSoaRecordService dnsSoaRecordService;

    @InjectMocks
    private UpdateDnsSoaRecordsHandler underTest;

    @Test
    void testAccept() throws Exception {
        String fqdn1 = "foo1.example.com";
        String fqdn2 = "foo2.example.com.";
        CleanupEvent cleanupEvent = new CleanupEvent(1L, Set.of(), Set.of(fqdn1, fqdn2), Set.of(), Set.of(), Set.of(), "", "", "", "");
        UpdateDnsSoaRecordsRequest request = new UpdateDnsSoaRecordsRequest(cleanupEvent);

        UpdateDnsSoaRecordsResponse result = (UpdateDnsSoaRecordsResponse) underTest.doAccept(new HandlerEvent<>(new Event<>(request)));

        ArgumentCaptor<Set<String>> fqdnCaptor = ArgumentCaptor.forClass(Set.class);
        verify(dnsSoaRecordService).updateSoaRecords(eq(1L), fqdnCaptor.capture());
        Set<String> fqdns = fqdnCaptor.getValue();
        assertTrue(fqdns.contains("foo1.example.com."));
        assertTrue(fqdns.contains("foo2.example.com."));
        assertEquals(2, fqdns.size());
        assertEquals(1L, result.getResourceId());
    }
}