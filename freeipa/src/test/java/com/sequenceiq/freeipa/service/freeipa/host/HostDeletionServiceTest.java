package com.sequenceiq.freeipa.service.freeipa.host;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.util.Pair;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Host;
import com.sequenceiq.freeipa.kerberosmgmt.exception.DeleteException;

@RunWith(MockitoJUnitRunner.class)
public class HostDeletionServiceTest {

    private static final int NOT_FOUND = 4001;

    @Mock
    private FreeIpaClient freeIpaClient;

    private HostDeletionService underTest;

    @Before
    public void init() {
        underTest = new HostDeletionService();
    }

    @Test(expected = FreeIpaClientException.class)
    public void expectFreeIpaClientExceptionIfHostCollectionFails() throws FreeIpaClientException {
        when(freeIpaClient.findAllHost()).thenThrow(new FreeIpaClientException("error"));
        underTest.removeHosts(freeIpaClient, Set.of("testHost"));
    }

    @Test(expected = DeleteException.class)
    public void expectDeleteExceptionIfHostCollectionFails() throws FreeIpaClientException {
        when(freeIpaClient.findAllHost()).thenThrow(new FreeIpaClientException("error"));
        underTest.deleteHostsWithDeleteException(freeIpaClient, Set.of("testHost"));
    }

    @Test
    public void successfulDeletionIfNoHostReturned() throws FreeIpaClientException {
        when(freeIpaClient.findAllHost()).thenReturn(Set.of());
        Set<String> hosts = Set.of("host1", "host2");

        Pair<Set<String>, Map<String, String>> result = underTest.removeHosts(freeIpaClient, hosts);

        assertTrue(result.getSecond().isEmpty());
        assertEquals(0, result.getFirst().size());
    }

    @Test
    public void successfulDeletionIfOneHostReturned() throws FreeIpaClientException {
        Set<String> hosts = Set.of("host1", "host2");
        Host host = new Host();
        host.setFqdn("host1");
        when(freeIpaClient.findAllHost()).thenReturn(Set.of(host));

        Pair<Set<String>, Map<String, String>> result = underTest.removeHosts(freeIpaClient, hosts);

        assertTrue(result.getSecond().isEmpty());
        assertEquals(1, result.getFirst().size());
        assertEquals(host.getFqdn(), result.getFirst().iterator().next());
    }

    @Test
    public void successfulDeletionIfAllHostReturned() throws FreeIpaClientException {
        Set<String> hosts = Set.of("host1", "host2");
        Host host1 = new Host();
        host1.setFqdn("host1");
        Host host2 = new Host();
        host2.setFqdn("host2");
        when(freeIpaClient.findAllHost()).thenReturn(Set.of(host1, host2));

        Pair<Set<String>, Map<String, String>> result = underTest.removeHosts(freeIpaClient, hosts);

        assertTrue(result.getSecond().isEmpty());
        assertEquals(2, result.getFirst().size());
        assertTrue(result.getFirst().contains(host1.getFqdn()));
        assertTrue(result.getFirst().contains(host2.getFqdn()));
    }

    @Test
    public void testOneFailedDeletion() throws FreeIpaClientException {
        Set<String> hosts = Set.of("host1", "host2");
        Host host1 = new Host();
        host1.setFqdn("host1");
        Host host2 = new Host();
        host2.setFqdn("host2");
        when(freeIpaClient.findAllHost()).thenReturn(Set.of(host1, host2));
        when(freeIpaClient.deleteHost(host2.getFqdn())).thenThrow(new FreeIpaClientException("not handled"));

        Pair<Set<String>, Map<String, String>> result = underTest.removeHosts(freeIpaClient, hosts);

        assertEquals(1, result.getFirst().size());
        assertEquals(1, result.getSecond().size());
        assertTrue(result.getFirst().contains(host1.getFqdn()));
        assertTrue(result.getSecond().keySet().contains(host2.getFqdn()));
        assertTrue(result.getSecond().values().contains("not handled"));
    }

    @Test
    public void testOneAlreadyDeleted() throws FreeIpaClientException {
        Set<String> hosts = Set.of("host1", "host2");
        Host host1 = new Host();
        host1.setFqdn("host1");
        Host host2 = new Host();
        host2.setFqdn("host2");
        when(freeIpaClient.findAllHost()).thenReturn(Set.of(host1, host2));
        String message = "already deleted";
        when(freeIpaClient.deleteHost(host2.getFqdn())).thenThrow(new FreeIpaClientException(message, new JsonRpcClientException(NOT_FOUND, message, null)));

        Pair<Set<String>, Map<String, String>> result = underTest.removeHosts(freeIpaClient, hosts);

        assertEquals(2, result.getFirst().size());
        assertEquals(0, result.getSecond().size());
        assertTrue(result.getFirst().contains(host1.getFqdn()));
        assertTrue(result.getFirst().contains(host2.getFqdn()));
    }

    @Test(expected = DeleteException.class)
    public void expectDeleteExceptionWhenOneFailedDeletion() throws FreeIpaClientException {
        Set<String> hosts = Set.of("host1", "host2");
        Host host1 = new Host();
        host1.setFqdn("host1");
        Host host2 = new Host();
        host2.setFqdn("host2");
        when(freeIpaClient.findAllHost()).thenReturn(Set.of(host1, host2));
        when(freeIpaClient.deleteHost(host2.getFqdn())).thenThrow(new FreeIpaClientException("not handled"));

        underTest.deleteHostsWithDeleteException(freeIpaClient, hosts);
    }
}