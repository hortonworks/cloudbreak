package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.DnsRecord;
import com.sequenceiq.freeipa.entity.FreeIpa;

@ExtendWith(MockitoExtension.class)
class DnsLoadBalanceSetupServiceTest {

    private static final int NOT_FOUND = 4001;

    private static final String DOMAIN = "example.com";

    private static final String CNAME = "ipa-ca.example.com";

    private static final Set<String> DNS_RECORDS = Set.of("ldap", "kdc", "kerberos", "freeipa");

    private static final DnsRecord DNS_RECORD = new DnsRecord();

    private static final FreeIpaClientException NOT_FOUND_EXCEPTION =
            new FreeIpaClientException("not found", new JsonRpcClientException(NOT_FOUND, "not found", null));

    private static final FreeIpa FREEIPA = new FreeIpa();

    @InjectMocks
    private DnsLoadBalanceSetupService underTest;

    @Mock
    private FreeIpaClient freeIpaClient;

    @BeforeAll
    static void setupFreeIpa() {
        FREEIPA.setDomain(DOMAIN);
    }

    @Test
    void testAddDnsLoadBalancedEntriesWhenTheyDontExist() throws FreeIpaClientException {
        Mockito.when(freeIpaClient.getDnsRecord(Mockito.any(), Mockito.any())).thenThrow(NOT_FOUND_EXCEPTION);
        Mockito.when(freeIpaClient.addDnsCnameRecord(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(DNS_RECORD);
        underTest.addDnsLoadBalancedEntries(freeIpaClient, FREEIPA);
        for (String dnsRecord : DNS_RECORDS) {
            Mockito.verify(freeIpaClient).addDnsCnameRecord(DOMAIN, dnsRecord, CNAME);
        }
    }

    @Test
    void testAddDnsLoadBalancedEntriesWhenTheyAlreadyExist() throws FreeIpaClientException {
        Mockito.when(freeIpaClient.getDnsRecord(Mockito.any(), Mockito.any())).thenReturn(DNS_RECORD);
        underTest.addDnsLoadBalancedEntries(freeIpaClient, FREEIPA);
        Mockito.verify(freeIpaClient, Mockito.never()).addDnsCnameRecord(Mockito.any(), Mockito.any(), Mockito.any());
    }

}