package com.sequenceiq.freeipa.service.freeipa.dns;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsARecordRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsCnameRecordRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaErrorCodes;
import com.sequenceiq.freeipa.client.model.DnsRecord;
import com.sequenceiq.freeipa.client.model.DnsZone;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class DnsRecordServiceTest {

    private static final String ENV_CRN = "env-crn";

    private static final String ACCOUNT_ID = "account-id";

    private static final String DOMAIN = "example.com";

    private static final String DOMAIN2 = "example.org";

    private static final String TARGET_FQDN = "example2.com.";

    private static final List<String> SRV_RECORDS = List.of("1 2 foo.example.com.");

    @Mock
    private CleanupService cleanupService;

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
        Stack stack = createStack();
        given(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).willReturn(stack);
        given(freeIpaService.findByStack(any())).willReturn(createFreeIpa());
        given(freeIpaClientFactory.getFreeIpaClientForStack(stack)).willReturn(freeIpaClient);
        given(freeIpaClient.findAllDnsZone()).willReturn(createDnsZones());
        given(freeIpaClient.findAllDnsRecordInZone(DOMAIN + '.')).willReturn(createDnsRecords());
        // WHEN
        underTest.deleteDnsRecordByFqdn(ENV_CRN, ACCOUNT_ID, List.of("www.example.com", "foo.example.com"));
        // THEN
        verify(freeIpaClient).deleteDnsRecord(eq("www.example.com"), eq("example.com."));
        verify(freeIpaClient).deleteDnsSrvRecord(eq("foo.example.com"), eq("example.com."), eq(SRV_RECORDS));
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setEnvironmentCrn(ENV_CRN);
        return stack;
    }

    private FreeIpa createFreeIpa() {
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        return freeIpa;
    }

    private Set<DnsZone> createDnsZones(String... domain) {
        return Arrays.stream(domain).map(d -> {
            DnsZone dnsZone = new DnsZone();
            dnsZone.setIdnsname(StringUtils.appendIfMissing(d, "."));
            return dnsZone;
        }).collect(Collectors.toSet());
    }

    private Set<DnsZone> createDnsZones() {
        return createDnsZones(DOMAIN);
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

    @Test
    public void testARecordAdd() throws FreeIpaClientException {
        AddDnsARecordRequest request = new AddDnsARecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setHostname("Asdf");
        request.setIp("1.1.1.2");
        request.setCreateReverse(true);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);

        underTest.addDnsARecord(ACCOUNT_ID, request);

        verify(freeIpaClient).addDnsARecord(DOMAIN, request.getHostname(), request.getIp(), request.isCreateReverse());
    }

    @Test
    public void testARecordAddEmptyModListIgnored() throws FreeIpaClientException {
        AddDnsARecordRequest request = new AddDnsARecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setHostname("Asdf");
        request.setIp("1.1.1.2");
        request.setCreateReverse(true);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        JsonRpcClientException noModEx = new JsonRpcClientException(FreeIpaErrorCodes.EMPTY_MODLIST.getValue(), "no modifications to be performed", null);
        when(freeIpaClient.addDnsARecord(DOMAIN, request.getHostname(), request.getIp(), request.isCreateReverse()))
                .thenThrow(new FreeIpaClientException("can't create", noModEx));

        underTest.addDnsARecord(ACCOUNT_ID, request);

        verify(freeIpaClient).addDnsARecord(DOMAIN, request.getHostname(), request.getIp(), request.isCreateReverse());
    }

    @Test
    public void testARecordAddNotFound() throws FreeIpaClientException {
        AddDnsARecordRequest request = new AddDnsARecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setHostname("Asdf");
        request.setIp("1.1.1.2");
        request.setCreateReverse(true);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.showDnsRecord(DOMAIN, request.getHostname()))
                .thenThrow(new FreeIpaClientException("Not found", new JsonRpcClientException(FreeIpaErrorCodes.NOT_FOUND.getValue(), "Not found", null)));

        underTest.addDnsARecord(ACCOUNT_ID, request);

        verify(freeIpaClient).addDnsARecord(DOMAIN, request.getHostname(), request.getIp(), request.isCreateReverse());
    }

    @Test
    public void testARecordAddSameDomain() throws FreeIpaClientException {
        AddDnsARecordRequest request = new AddDnsARecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setHostname("Asdf");
        request.setIp("1.1.1.2");
        request.setDnsZone(DOMAIN);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);

        underTest.addDnsARecord(ACCOUNT_ID, request);

        verify(freeIpaClient).addDnsARecord(request.getDnsZone(), request.getHostname(), request.getIp(), request.isCreateReverse());
    }

    @Test
    public void testARecordAddDifferentDomainExists() throws FreeIpaClientException {
        AddDnsARecordRequest request = new AddDnsARecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setHostname("Asdf");
        request.setIp("1.1.1.2");
        request.setDnsZone(DOMAIN2);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.findAllDnsZone()).thenReturn(createDnsZones(DOMAIN, DOMAIN2));

        underTest.addDnsARecord(ACCOUNT_ID, request);

        verify(freeIpaClient).addDnsARecord(request.getDnsZone(), request.getHostname(), request.getIp(), request.isCreateReverse());
    }

    @Test
    public void testARecordAddDifferentDomainMissing() throws FreeIpaClientException {
        AddDnsARecordRequest request = new AddDnsARecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setHostname("Asdf");
        request.setIp("1.1.1.2");
        request.setDnsZone(DOMAIN2);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.findAllDnsZone()).thenReturn(createDnsZones(DOMAIN));

        assertThrows(BadRequestException.class, () -> underTest.addDnsARecord(ACCOUNT_ID, request));
    }

    @Test
    public void testARecordExists() throws FreeIpaClientException {
        AddDnsARecordRequest request = new AddDnsARecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setHostname("Asdf");
        request.setIp("1.1.1.2");
        request.setCreateReverse(true);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        DnsRecord dnsRecord = new DnsRecord();
        dnsRecord.setArecord(List.of(request.getIp()));
        dnsRecord.setIdnsname(request.getHostname());
        when(freeIpaClient.showDnsRecord(DOMAIN, request.getHostname())).thenReturn(dnsRecord);

        underTest.addDnsARecord(ACCOUNT_ID, request);

        verify(freeIpaClient, times(0)).addDnsARecord(DOMAIN, request.getHostname(), request.getIp(), request.isCreateReverse());
    }

    @Test
    public void testARecordExistsNotA() throws FreeIpaClientException {
        AddDnsARecordRequest request = new AddDnsARecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setHostname("Asdf");
        request.setIp("1.1.1.2");
        request.setCreateReverse(true);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        DnsRecord dnsRecord = new DnsRecord();
        dnsRecord.setPtrrecord(List.of("asdf"));
        dnsRecord.setIdnsname(request.getHostname());
        when(freeIpaClient.showDnsRecord(DOMAIN, request.getHostname())).thenReturn(dnsRecord);

        assertThrows(DnsRecordConflictException.class, () -> underTest.addDnsARecord(ACCOUNT_ID, request));
    }

    @Test
    public void testARecordExistsDifferentValue() throws FreeIpaClientException {
        AddDnsARecordRequest request = new AddDnsARecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setHostname("Asdf");
        request.setIp("1.1.1.2");
        request.setCreateReverse(true);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        DnsRecord dnsRecord = new DnsRecord();
        dnsRecord.setArecord(List.of("asdf"));
        dnsRecord.setIdnsname(request.getHostname());
        when(freeIpaClient.showDnsRecord(DOMAIN, request.getHostname())).thenReturn(dnsRecord);

        assertThrows(DnsRecordConflictException.class, () -> underTest.addDnsARecord(ACCOUNT_ID, request));
    }

    @Test
    public void testARecordCreateReturnDuplicate() throws FreeIpaClientException {
        AddDnsARecordRequest request = new AddDnsARecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setHostname("Asdf");
        request.setIp("1.1.1.2");
        request.setCreateReverse(true);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.addDnsARecord(anyString(), eq(request.getHostname()), eq(request.getIp()), eq(true)))
                .thenThrow(new FreeIpaClientException("Duplicate", new JsonRpcClientException(4002, "Duplicate reverse", null)));

        assertThrows(DnsRecordConflictException.class, () -> underTest.addDnsARecord(ACCOUNT_ID, request));
    }

    @Test
    public void testARecordExistsWithDifferentValueAndForceIsTrue() throws FreeIpaClientException {
        AddDnsARecordRequest request = new AddDnsARecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setHostname("cloudera-master");
        request.setIp("8.8.8.8");
        request.setCreateReverse(true);
        request.setForce(true);
        Stack stack = createStack();
        FreeIpa freeIpa = createFreeIpa();
        DnsRecord dnsRecord = new DnsRecord();
        dnsRecord.setArecord(List.of("1.1.1.1"));
        dnsRecord.setIdnsname(request.getHostname());

        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.showDnsRecord(DOMAIN, request.getHostname())).thenReturn(dnsRecord);

        underTest.addDnsARecord(ACCOUNT_ID, request);

        verify(cleanupService).removeDnsEntries(eq(freeIpaClient), anySet(), anySet(), eq(DOMAIN), eq(ENV_CRN));
        verify(freeIpaClient).addDnsARecord(DOMAIN, request.getHostname(), request.getIp(), request.isCreateReverse());
    }

    @Test
    public void testARecordExistsWithSameValueAndForceIsTrue() throws FreeIpaClientException {
        AddDnsARecordRequest request = new AddDnsARecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setHostname("cloudera-master");
        request.setIp("1.1.1.1");
        request.setCreateReverse(true);
        request.setForce(true);
        Stack stack = createStack();
        FreeIpa freeIpa = createFreeIpa();
        DnsRecord dnsRecord = new DnsRecord();
        dnsRecord.setArecord(List.of("1.1.1.1"));
        dnsRecord.setIdnsname(request.getHostname());

        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.showDnsRecord(DOMAIN, request.getHostname())).thenReturn(dnsRecord);

        underTest.addDnsARecord(ACCOUNT_ID, request);

        verifyNoInteractions(cleanupService);
        verify(freeIpaClient, times(0)).addDnsARecord(DOMAIN, request.getHostname(), request.getIp(), request.isCreateReverse());
    }

    @Test
    public void testARecordExistsWithOtherValueAndForceIsFalse() throws FreeIpaClientException {
        AddDnsARecordRequest request = new AddDnsARecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setHostname("cloudera-master");
        request.setIp("8.8.8.8");
        request.setCreateReverse(true);
        request.setForce(false);
        Stack stack = createStack();
        FreeIpa freeIpa = createFreeIpa();
        DnsRecord dnsRecord = new DnsRecord();
        dnsRecord.setArecord(List.of("1.1.1.1"));
        dnsRecord.setIdnsname(request.getHostname());

        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.showDnsRecord(DOMAIN, request.getHostname())).thenReturn(dnsRecord);

        assertThrows(DnsRecordConflictException.class, () -> underTest.addDnsARecord(ACCOUNT_ID, request));

        verifyNoInteractions(cleanupService);
        verify(freeIpaClient, times(0)).addDnsARecord(DOMAIN, request.getHostname(), request.getIp(), request.isCreateReverse());
    }

    @Test
    public void testARecordNotExistsAndForceIsTrue() throws FreeIpaClientException {
        AddDnsARecordRequest request = new AddDnsARecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setHostname("cloudera-master");
        request.setIp("8.8.8.8");
        request.setCreateReverse(true);
        request.setForce(true);
        Stack stack = createStack();
        FreeIpa freeIpa = createFreeIpa();

        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.showDnsRecord(DOMAIN, request.getHostname())).thenReturn(null);

        underTest.addDnsARecord(ACCOUNT_ID, request);

        verify(cleanupService).removeDnsEntries(eq(freeIpaClient), anySet(), anySet(), eq(DOMAIN), eq(ENV_CRN));
        verify(freeIpaClient).addDnsARecord(DOMAIN, request.getHostname(), request.getIp(), request.isCreateReverse());
    }

    @Test
    public void testARecordNotExistsAndForceIsFalse() throws FreeIpaClientException {
        AddDnsARecordRequest request = new AddDnsARecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setHostname("cloudera-master");
        request.setIp("8.8.8.8");
        request.setCreateReverse(true);
        request.setForce(false);
        Stack stack = createStack();
        FreeIpa freeIpa = createFreeIpa();

        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.showDnsRecord(DOMAIN, request.getHostname())).thenReturn(null);

        underTest.addDnsARecord(ACCOUNT_ID, request);

        verifyNoInteractions(cleanupService);
        verify(freeIpaClient).addDnsARecord(DOMAIN, request.getHostname(), request.getIp(), request.isCreateReverse());
    }

    @Test
    public void testCnameRecordAdd() throws FreeIpaClientException {
        AddDnsCnameRecordRequest request = new AddDnsCnameRecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setCname("Asdf");
        request.setTargetFqdn(TARGET_FQDN);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);

        underTest.addDnsCnameRecord(ACCOUNT_ID, request);

        verify(freeIpaClient).addDnsCnameRecord(DOMAIN, request.getCname(), request.getTargetFqdn());
    }

    @Test
    public void testCnameRecordAddEmptyModListIgnored() throws FreeIpaClientException {
        AddDnsCnameRecordRequest request = new AddDnsCnameRecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setCname("Asdf");
        request.setTargetFqdn(TARGET_FQDN);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        JsonRpcClientException noModEx = new JsonRpcClientException(FreeIpaErrorCodes.EMPTY_MODLIST.getValue(), "no modifications to be performed", null);
        when(freeIpaClient.addDnsCnameRecord(DOMAIN, request.getCname(), request.getTargetFqdn()))
                .thenThrow(new FreeIpaClientException("can't create", noModEx));

        underTest.addDnsCnameRecord(ACCOUNT_ID, request);

        verify(freeIpaClient).addDnsCnameRecord(DOMAIN, request.getCname(), request.getTargetFqdn());
    }

    @Test
    public void testCnameRecordAddWithoutTrailingDot() throws FreeIpaClientException {
        AddDnsCnameRecordRequest request = new AddDnsCnameRecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setCname("Asdf");
        request.setTargetFqdn("example2.com");

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);

        underTest.addDnsCnameRecord(ACCOUNT_ID, request);

        verify(freeIpaClient).addDnsCnameRecord(DOMAIN, request.getCname(), TARGET_FQDN);
    }

    @Test
    public void testCnameRecordAddNotFound() throws FreeIpaClientException {
        AddDnsCnameRecordRequest request = new AddDnsCnameRecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setCname("Asdf");
        request.setTargetFqdn(TARGET_FQDN);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.showDnsRecord(DOMAIN, request.getCname()))
                .thenThrow(new FreeIpaClientException("Not found", new JsonRpcClientException(FreeIpaErrorCodes.NOT_FOUND.getValue(), "Not found", null)));

        underTest.addDnsCnameRecord(ACCOUNT_ID, request);

        verify(freeIpaClient).addDnsCnameRecord(DOMAIN, request.getCname(), request.getTargetFqdn());
    }

    @Test
    public void testCnameRecordAddSameDomain() throws FreeIpaClientException {
        AddDnsCnameRecordRequest request = new AddDnsCnameRecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setCname("Asdf");
        request.setTargetFqdn(TARGET_FQDN);
        request.setDnsZone(DOMAIN);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);

        underTest.addDnsCnameRecord(ACCOUNT_ID, request);

        verify(freeIpaClient).addDnsCnameRecord(DOMAIN, request.getCname(), request.getTargetFqdn());
    }

    @Test
    public void testCnameRecordAddDifferentDomainExists() throws FreeIpaClientException {
        AddDnsCnameRecordRequest request = new AddDnsCnameRecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setCname("Asdf");
        request.setTargetFqdn(TARGET_FQDN);
        request.setDnsZone(DOMAIN2);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.findAllDnsZone()).thenReturn(createDnsZones(DOMAIN, DOMAIN2));

        underTest.addDnsCnameRecord(ACCOUNT_ID, request);

        verify(freeIpaClient).addDnsCnameRecord(request.getDnsZone(), request.getCname(), request.getTargetFqdn());
    }

    @Test
    public void testCnameRecordAddDifferentDomainMissing() throws FreeIpaClientException {
        AddDnsCnameRecordRequest request = new AddDnsCnameRecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setCname("Asdf");
        request.setTargetFqdn(TARGET_FQDN);
        request.setDnsZone(DOMAIN2);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.findAllDnsZone()).thenReturn(createDnsZones(DOMAIN));

        assertThrows(BadRequestException.class, () -> underTest.addDnsCnameRecord(ACCOUNT_ID, request),
                String.format("Zone [%s] doesn't exists", DOMAIN2));
    }

    @Test
    public void testCnameRecordExists() throws FreeIpaClientException {
        AddDnsCnameRecordRequest request = new AddDnsCnameRecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setCname("Asdf");
        request.setTargetFqdn(TARGET_FQDN);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        DnsRecord dnsRecord = new DnsRecord();
        dnsRecord.setCnamerecord(List.of(request.getTargetFqdn()));
        dnsRecord.setIdnsname(request.getCname());
        when(freeIpaClient.showDnsRecord(DOMAIN, request.getCname())).thenReturn(dnsRecord);

        underTest.addDnsCnameRecord(ACCOUNT_ID, request);

        verify(freeIpaClient, times(0)).addDnsCnameRecord(DOMAIN, request.getCname(), request.getTargetFqdn());
    }

    @Test
    public void testCnameRecordExistsNotCname() throws FreeIpaClientException {
        AddDnsCnameRecordRequest request = new AddDnsCnameRecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setCname("Asdf");
        request.setTargetFqdn(TARGET_FQDN);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        DnsRecord dnsRecord = new DnsRecord();
        dnsRecord.setPtrrecord(List.of("asdf"));
        dnsRecord.setIdnsname(request.getCname());
        when(freeIpaClient.showDnsRecord(DOMAIN, request.getCname())).thenReturn(dnsRecord);

        assertThrows(DnsRecordConflictException.class, () -> underTest.addDnsCnameRecord(ACCOUNT_ID, request));
    }

    @Test
    public void testCnameRecordExistsDifferentValue() throws FreeIpaClientException {
        AddDnsCnameRecordRequest request = new AddDnsCnameRecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setCname("Asdf");
        request.setTargetFqdn(TARGET_FQDN);

        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        DnsRecord dnsRecord = new DnsRecord();
        dnsRecord.setCnamerecord(List.of("asdf"));
        dnsRecord.setIdnsname(request.getCname());
        when(freeIpaClient.showDnsRecord(DOMAIN, request.getCname())).thenReturn(dnsRecord);

        assertThrows(DnsRecordConflictException.class, () -> underTest.addDnsCnameRecord(ACCOUNT_ID, request));
    }

    @Test
    public void testCnameRecordExistsWithDifferentValueAndForceIsTrue() throws FreeIpaClientException {
        AddDnsCnameRecordRequest request = new AddDnsCnameRecordRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setCname("cloudera-gateway");
        request.setTargetFqdn(TARGET_FQDN);
        request.setForce(true);
        Stack stack = createStack();
        FreeIpa freeIpa = createFreeIpa();
        DnsRecord dnsRecord = new DnsRecord();
        dnsRecord.setCnamerecord(List.of("loadbalancer.com"));
        dnsRecord.setIdnsname(request.getCname());

        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.showDnsRecord(DOMAIN, request.getCname())).thenReturn(dnsRecord);

        underTest.addDnsCnameRecord(ACCOUNT_ID, request);

        verify(freeIpaClient).deleteDnsRecord(eq(request.getCname()), anyString());
        verify(freeIpaClient).addDnsCnameRecord(DOMAIN, request.getCname(), request.getTargetFqdn());
    }

    @Test
    public void testDelete() throws FreeIpaClientException {
        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);

        underTest.deleteDnsRecord(ACCOUNT_ID, ENV_CRN, null, "asdf");

        verify(freeIpaClient).deleteDnsRecord("asdf", DOMAIN);
    }

    @Test
    public void testDeleteSameDomain() throws FreeIpaClientException {
        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);

        underTest.deleteDnsRecord(ACCOUNT_ID, ENV_CRN, DOMAIN, "asdf");

        verify(freeIpaClient).deleteDnsRecord("asdf", DOMAIN);
    }

    @Test
    public void testDeleteDifferentDomain() throws FreeIpaClientException {
        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.findAllDnsZone()).thenReturn(createDnsZones(DOMAIN, DOMAIN2));

        underTest.deleteDnsRecord(ACCOUNT_ID, ENV_CRN, DOMAIN2, "asdf");

        verify(freeIpaClient).deleteDnsRecord("asdf", DOMAIN2);
    }

    @Test
    public void testDeleteDifferentDomainMissing() throws FreeIpaClientException {
        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.findAllDnsZone()).thenReturn(createDnsZones(DOMAIN));

        assertThrows(BadRequestException.class, () -> underTest.deleteDnsRecord(ACCOUNT_ID, ENV_CRN, DOMAIN2, "asdf"));
    }

    @Test
    public void testDeleteIgnoreNotFound() throws FreeIpaClientException {
        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.deleteDnsRecord("asdf", DOMAIN))
                .thenThrow(new FreeIpaClientException("Not found", new JsonRpcClientException(FreeIpaErrorCodes.NOT_FOUND.getValue(), "Not found", null)));

        underTest.deleteDnsRecord(ACCOUNT_ID, ENV_CRN, null, "asdf");
    }
}
