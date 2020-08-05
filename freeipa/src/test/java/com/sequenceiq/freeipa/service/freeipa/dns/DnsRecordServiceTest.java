package com.sequenceiq.freeipa.service.freeipa.dns;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.model.DnsRecord;
import com.sequenceiq.freeipa.client.model.DnsZone;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class DnsRecordServiceTest {

    private static final String ENV_CRN = "env-crn";

    private static final String ACCOUNT_ID = "account-id";

    private static final String DOMAIN = "example.com";

    private static final List<String> SRV_RECORDS = List.of("1 2 foo.example.com.");

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaClient freeIpaClient;

    @InjectMocks
    private DnsRecordService underTest;

    @Test
    public void testDeleteDnsRecordByFqdn() throws Exception {
        // GIVEN
        given(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).willReturn(createStack());
        given(freeIpaService.findByStack(any())).willReturn(createFreeIpa());
        given(freeIpaClientFactory.getFreeIpaClientByAccountAndEnvironment(ENV_CRN, ACCOUNT_ID)).willReturn(freeIpaClient);
        given(freeIpaClient.findAllDnsZone()).willReturn(createDnsZones());
        given(freeIpaClient.findAllDnsRecordInZone(DOMAIN)).willReturn(createDnsRecords());
        // WHEN
        underTest.deleteDnsRecordByFqdn(ENV_CRN, ACCOUNT_ID, List.of("www.example.com", "foo.example.com"));
        // THEN
        verify(freeIpaClient).deleteDnsRecord(eq("www.example.com"), eq("example.com"));
        verify(freeIpaClient).deleteDnsSrvRecord(eq("foo.example.com"), eq("example.com"), eq(SRV_RECORDS));
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(1L);
        return stack;
    }

    private FreeIpa createFreeIpa() {
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        return freeIpa;
    }

    private Set<DnsZone> createDnsZones() {
        DnsZone dnsZone = new DnsZone();
        dnsZone.setIdnsname(DOMAIN);
        return Set.of(dnsZone);
    }

    private Set<DnsRecord> createDnsRecords() {
        DnsRecord dnsRecord = new DnsRecord();
        dnsRecord.setIdnsname("www.example.com");
        dnsRecord.setPtrrecord(List.of("www.example.com."));
        DnsRecord srvRecord = new DnsRecord();
        srvRecord.setIdnsname("foo.example.com");
        srvRecord.setSrvrecord(SRV_RECORDS);
        return Set.of(dnsRecord, srvRecord);
    }
}
