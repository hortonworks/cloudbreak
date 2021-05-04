package com.sequenceiq.freeipa.service.cleanup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.model.TopologySegment;
import com.sequenceiq.freeipa.client.model.TopologySuffix;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.cleanup.FreeIpaServerDeletionListenerTask;
import com.sequenceiq.freeipa.service.freeipa.cleanup.FreeIpaServerDeletionPollerObject;

@ExtendWith(MockitoExtension.class)
class FreeIplaServiceDeletionListenerTaskTest {

    @InjectMocks
    private FreeIpaServerDeletionListenerTask underTest;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Test
    void testCheckStatusTrue() throws Exception {
        FreeIpaServerDeletionPollerObject pollerObject = new FreeIpaServerDeletionPollerObject(1L, Set.of("example0.com"));
        FreeIpaClient client = mock(FreeIpaClient.class);
        TopologySuffix suffix = new TopologySuffix();
        suffix.setCn("ca");
        TopologySegment segment = new TopologySegment();
        segment.setRightNode("example1.com");
        segment.setLeftNode("example2.com");
        when(freeIpaClientFactory.getFreeIpaClientForStackId(anyLong())).thenReturn(client);
        when(client.findAllTopologySuffixes()).thenReturn(List.of(suffix));
        when(client.findTopologySegments(any())).thenReturn(List.of(segment));

        assertTrue(underTest.checkStatus(pollerObject));
    }

    @Test
    void testCheckStatusFalse() throws Exception {
        FreeIpaServerDeletionPollerObject pollerObject = new FreeIpaServerDeletionPollerObject(1L, Set.of("example1.com"));
        FreeIpaClient client = mock(FreeIpaClient.class);
        TopologySuffix suffix = new TopologySuffix();
        suffix.setCn("ca");
        TopologySegment segment = new TopologySegment();
        segment.setRightNode("example1.com");
        segment.setLeftNode("example2.com");
        when(freeIpaClientFactory.getFreeIpaClientForStackId(anyLong())).thenReturn(client);
        when(client.findAllTopologySuffixes()).thenReturn(List.of(suffix));
        when(client.findTopologySegments(any())).thenReturn(List.of(segment));

        assertFalse(underTest.checkStatus(pollerObject));
    }
}