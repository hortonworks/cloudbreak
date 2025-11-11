package com.sequenceiq.freeipa.service.freeipa.dns;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.OptionalInt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.node.IntNode;
import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaErrorCodes;
import com.sequenceiq.freeipa.client.model.DnsZone;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@ExtendWith(MockitoExtension.class)
class DnsZoneServiceTest {

    @InjectMocks
    private DnsZoneService underTest;

    @Mock
    private FreeIpaClientFactory ipaClientFactory;

    @Mock
    private Stack stack;

    @Mock
    private CrossRealmTrust crossRealmTrust;

    @Mock
    private FreeIpaClient ipaClient;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testAddDnsForwardZone() throws Exception {
        String forwardZone = "hybrid.cloudera.org";
        when(crossRealmTrust.getKdcRealm()).thenReturn("hybrid.cloudera.org");

        String dnsIp = "10.0.0.1";
        when(crossRealmTrust.getDnsIp()).thenReturn(dnsIp);

        String forwardPolicy = "only";

        JsonRpcClientException cause =
                new JsonRpcClientException(FreeIpaErrorCodes.NOT_FOUND.getValue(), "", new IntNode(FreeIpaErrorCodes.NOT_FOUND.getValue()));
        when(ipaClient.showForwardDnsZone(forwardZone))
                .thenThrow(new FreeIpaClientException("Not found", cause, OptionalInt.of(FreeIpaErrorCodes.NOT_FOUND.getValue())));

        when(ipaClientFactory.getFreeIpaClientForStackIgnoreUnreachable(stack)).thenReturn(ipaClient);
        underTest.addDnsForwardZone(ipaClientFactory, stack, crossRealmTrust);
        verify(ipaClient).addForwardDnsZone(forwardZone, dnsIp, forwardPolicy);
    }

    @Test
    void testModDnsForwardZone() throws Exception {
        String forwardZone = "hybrid.cloudera.org";
        when(crossRealmTrust.getKdcRealm()).thenReturn("hybrid.cloudera.org");

        String dnsIp = "10.0.0.1";
        when(crossRealmTrust.getDnsIp()).thenReturn(dnsIp);

        String forwardPolicy = "only";

        DnsZone dnsZone = mock(DnsZone.class);
        when(ipaClient.showForwardDnsZone(forwardZone)).thenReturn(dnsZone);

        when(ipaClientFactory.getFreeIpaClientForStackIgnoreUnreachable(stack)).thenReturn(ipaClient);
        underTest.addDnsForwardZone(ipaClientFactory, stack, crossRealmTrust);
        verify(ipaClient).modForwardDnsZone(forwardZone, dnsIp, forwardPolicy);
    }
}