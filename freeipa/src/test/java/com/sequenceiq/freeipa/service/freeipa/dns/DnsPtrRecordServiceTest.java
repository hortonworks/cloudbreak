package com.sequenceiq.freeipa.service.freeipa.dns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsPtrRecordRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.DeleteDnsPtrRecordRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.DnsRecord;
import com.sequenceiq.freeipa.client.model.DnsZone;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class DnsPtrRecordServiceTest {
    private static final String ENV_CRN = "env-crn";

    private static final String ACCOUNT_ID = "account-id";

    private static final String DOMAIN = "example.com";

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaClient freeIpaClient;

    @InjectMocks
    private DnsPtrRecordService underTest;

    public static Stream<Arguments> dnsRecordParams() {
        return Stream.of(
                //ip, reverseDnsZone, fqdn, ptrName
                Arguments.of("2.1.in-addr.arpa", "1.2.3.4", "fqdn", "4.3"),
                Arguments.of("20.10.in-addr.arpa.", "10.20.30.40", "fqdn", "40.30"),
                Arguments.of("1.in-addr.arpa", "1.2.3.4", "fqdn.", "4.3.2"),
                Arguments.of("33.24.10.in-addr.arpa.", "10.24.33.4", "fqdn.", "4")
        );
    }

    public static Stream<Arguments> wrongDnsRecordParams() {
        return Stream.of(
                //ip, reverseDnsZone, fqdn
                Arguments.of("2.1.in-addr.arpa", "1.1.3.4", "fqdn"),
                Arguments.of("20.10.in-addr.arpa.", "20.20.30.40", "fqdn"),
                Arguments.of("1.in-addr.arpa", "21.23.3.4", "fqdn."),
                Arguments.of("33.24.10.in-addr.arpa.", "10.25.33.4", "fqdn.")
        );
    }

    public static Stream<Arguments> dnsRecordParamsNoZoneGiven() {
        return Stream.of(
                //ip, fqdn, dnsZones, properZone, ptrName
                Arguments.of("1.2.3.4", "fqdn", List.of("2.1.in-addr.arpa", "1.in-addr.arpa", "3.2.2.in-addr.arpa", "noreverse1", "noreverse2"),
                        "2.1.in-addr.arpa.", "4.3"),
                Arguments.of("10.20.30.40", "fqdn", List.of("20.10.in-addr.arpa", "1.in-addr.arpa", "3.2.1.in-addr.arpa", "noreverse1"),
                        "20.10.in-addr.arpa", "40.30"),
                Arguments.of("1.2.3.4", "fqdn.", List.of("2.1.in-addr.arpa", "1.in-addr.arpa", "3.2.1.in-addr.arpa", "noreverse1", "noreverse2"),
                        "3.2.1.in-addr.arpa.", "4")
        );
    }

    @ParameterizedTest
    @MethodSource("dnsRecordParams")
    public void testAddDnsPtrRecord(String reverseZone, String ip, String fqdn, String ptrRecordName) throws FreeIpaClientException {
        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        AddDnsPtrRecordRequest addDnsPtrRecordRequest = new AddDnsPtrRecordRequest();
        addDnsPtrRecordRequest.setEnvironmentCrn(ENV_CRN);
        addDnsPtrRecordRequest.setIp(ip);
        addDnsPtrRecordRequest.setReverseDnsZone(reverseZone);
        addDnsPtrRecordRequest.setFqdn(fqdn);
        underTest.addDnsPtrRecord(addDnsPtrRecordRequest, ACCOUNT_ID);
        verify(freeIpaClient).addDnsPtrRecord(StringUtils.appendIfMissing(reverseZone, "."), ptrRecordName, StringUtils.appendIfMissing(fqdn, "."));
    }

    @ParameterizedTest
    @MethodSource("wrongDnsRecordParams")
    public void testAddDnsPtrRecordNoMatchingZone(String reverseZone, String ip, String fqdn) throws FreeIpaClientException {
        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        AddDnsPtrRecordRequest addDnsPtrRecordRequest = new AddDnsPtrRecordRequest();
        addDnsPtrRecordRequest.setEnvironmentCrn(ENV_CRN);
        addDnsPtrRecordRequest.setIp(ip);
        addDnsPtrRecordRequest.setReverseDnsZone(reverseZone);
        addDnsPtrRecordRequest.setFqdn(fqdn);
        FreeIpaClientException actualException = assertThrows(FreeIpaClientException.class,
                () -> underTest.addDnsPtrRecord(addDnsPtrRecordRequest, ACCOUNT_ID));
        assertEquals(String.format("Reverse dns zone %s is not matching with ip %s", StringUtils.appendIfMissing(reverseZone, "."), ip),
                actualException.getMessage());
    }

    @ParameterizedTest
    @MethodSource("dnsRecordParamsNoZoneGiven")
    public void testAddDnsPtrRecordNoZoneGiven(String ip, String fqdn, List<String> freeipaZones, String properZone, String ptrRecordName)
            throws FreeIpaClientException {
        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        AddDnsPtrRecordRequest addDnsPtrRecordRequest = new AddDnsPtrRecordRequest();
        addDnsPtrRecordRequest.setEnvironmentCrn(ENV_CRN);
        addDnsPtrRecordRequest.setIp(ip);
        addDnsPtrRecordRequest.setFqdn(fqdn);
        when(freeIpaClient.findAllDnsZone()).thenReturn(createDnsZones(freeipaZones));
        underTest.addDnsPtrRecord(addDnsPtrRecordRequest, ACCOUNT_ID);
        verify(freeIpaClient).addDnsPtrRecord(StringUtils.appendIfMissing(properZone, "."), ptrRecordName, StringUtils.appendIfMissing(fqdn, "."));
    }

    @Test
    public void testAddDnsPtrRecordWhenRecordExists() throws FreeIpaClientException {
        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        AddDnsPtrRecordRequest addDnsPtrRecordRequest = new AddDnsPtrRecordRequest();
        addDnsPtrRecordRequest.setEnvironmentCrn(ENV_CRN);
        addDnsPtrRecordRequest.setIp("1.2.3.4");
        addDnsPtrRecordRequest.setReverseDnsZone("2.1.in-addr.arpa");
        addDnsPtrRecordRequest.setFqdn("fqdn");
        when(freeIpaClient.showDnsRecord("2.1.in-addr.arpa.", "4.3")).thenReturn(createPtrRecord("4.3", "fqdn."));
        underTest.addDnsPtrRecord(addDnsPtrRecordRequest, ACCOUNT_ID);
        verify(freeIpaClient, never()).addDnsPtrRecord(anyString(), anyString(), anyString());
    }

    @Test
    public void testAddDnsPtrRecordWhenRecordExistsButDifferent() throws FreeIpaClientException {
        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        AddDnsPtrRecordRequest addDnsPtrRecordRequest = new AddDnsPtrRecordRequest();
        addDnsPtrRecordRequest.setEnvironmentCrn(ENV_CRN);
        addDnsPtrRecordRequest.setIp("1.2.3.4");
        addDnsPtrRecordRequest.setReverseDnsZone("2.1.in-addr.arpa");
        addDnsPtrRecordRequest.setFqdn("fqdn");
        when(freeIpaClient.showDnsRecord("2.1.in-addr.arpa.", "4.3")).thenReturn(createPtrRecord("4.3", "different."));
        DnsRecordConflictException actualException = assertThrows(DnsRecordConflictException.class,
                () -> underTest.addDnsPtrRecord(addDnsPtrRecordRequest, ACCOUNT_ID));
        assertEquals("PTR record already exists and the target doesn't match", actualException.getMessage());
    }

    @Test
    public void testAddDnsPtrRecordNoZoneGivenAndNotFound() throws FreeIpaClientException {
        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        AddDnsPtrRecordRequest addDnsPtrRecordRequest = new AddDnsPtrRecordRequest();
        addDnsPtrRecordRequest.setEnvironmentCrn(ENV_CRN);
        addDnsPtrRecordRequest.setIp("1.2.3.4");
        addDnsPtrRecordRequest.setFqdn("fqdn");
        when(freeIpaClient.findAllDnsZone()).thenReturn(createDnsZones("2.2.in-addr.arpa", "noreverse"));
        FreeIpaClientException actualException = assertThrows(FreeIpaClientException.class,
                () -> underTest.addDnsPtrRecord(addDnsPtrRecordRequest, ACCOUNT_ID));
        assertEquals("No matching reverse dns zone found for 1.2.3.4 ip", actualException.getMessage());
    }

    @Test
    public void testDeleteDnsPtrRecord() throws FreeIpaClientException {
        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        DeleteDnsPtrRecordRequest deleteDnsPtrRecordRequest = new DeleteDnsPtrRecordRequest();
        deleteDnsPtrRecordRequest.setEnvironmentCrn(ENV_CRN);
        deleteDnsPtrRecordRequest.setIp("1.2.3.4");
        deleteDnsPtrRecordRequest.setReverseDnsZone("2.1.in-addr.arpa");
        underTest.deleteDnsPtrRecord(deleteDnsPtrRecordRequest, ACCOUNT_ID);
        verify(freeIpaClient).deleteDnsRecord("4.3", "2.1.in-addr.arpa.");
    }

    @Test
    public void testDeleteDnsPtrRecordWithNoZone() throws FreeIpaClientException {
        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        DeleteDnsPtrRecordRequest deleteDnsPtrRecordRequest = new DeleteDnsPtrRecordRequest();
        deleteDnsPtrRecordRequest.setEnvironmentCrn(ENV_CRN);
        deleteDnsPtrRecordRequest.setIp("1.2.3.4");
        when(freeIpaClient.findAllDnsZone()).thenReturn(createDnsZones("2.2.in-addr.arpa", "noreverse", "2.1.in-addr.arpa", "1.in-addr.arpa"));
        underTest.deleteDnsPtrRecord(deleteDnsPtrRecordRequest, ACCOUNT_ID);
        verify(freeIpaClient).deleteDnsRecord("4.3", "2.1.in-addr.arpa.");
        verify(freeIpaClient).deleteDnsRecord("4.3.2", "1.in-addr.arpa.");
    }

    @Test
    public void testAddDnsPtrRecordNoMatchingZone() throws FreeIpaClientException {
        Stack stack = createStack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        FreeIpa freeIpa = createFreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        DeleteDnsPtrRecordRequest deleteDnsPtrRecordRequest = new DeleteDnsPtrRecordRequest();
        deleteDnsPtrRecordRequest.setEnvironmentCrn(ENV_CRN);
        deleteDnsPtrRecordRequest.setIp("1.2.3.4");
        deleteDnsPtrRecordRequest.setReverseDnsZone("2.2.in-addr-arpa");
        FreeIpaClientException actualException = assertThrows(FreeIpaClientException.class,
                () -> underTest.deleteDnsPtrRecord(deleteDnsPtrRecordRequest, ACCOUNT_ID));
        assertEquals("Reverse dns zone 2.2.in-addr-arpa. is not matching with ip 1.2.3.4", actualException.getMessage());
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

    private Set<DnsZone> createDnsZones(String... domain) {
        return Arrays.stream(domain).map(d -> {
            DnsZone dnsZone = new DnsZone();
            dnsZone.setIdnsname(StringUtils.appendIfMissing(d, "."));
            return dnsZone;
        }).collect(Collectors.toSet());
    }

    private DnsRecord createPtrRecord(String name, String fqdn) {
        DnsRecord dnsRecord = new DnsRecord();
        dnsRecord.setIdnsname(name);
        dnsRecord.setPtrrecord(List.of(fqdn));
        return dnsRecord;
    }

    private Set<DnsZone> createDnsZones(List<String> domains) {
        return domains.stream().map(d -> {
            DnsZone dnsZone = new DnsZone();
            dnsZone.setIdnsname(StringUtils.appendIfMissing(d, "."));
            return dnsZone;
        }).collect(Collectors.toSet());
    }
}
