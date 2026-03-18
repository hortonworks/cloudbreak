package com.sequenceiq.freeipa.service.loadbalancer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.PemDnsEntryCreateOrUpdateException;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsPtrRecordRequest;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsPtrRecordService;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsRecordConflictException;

@ExtendWith(MockitoExtension.class)
class FreeIpaLoadBalancerDomainServiceTest {

    private static final long STACK_ID = 1L;

    private static final String DOMAIN = "test.example.com";

    private static final String ENDPOINT = "lb";

    private static final String IP = "10.0.0.5";

    private static final String EXPECTED_FQDN = ENDPOINT + "." + DOMAIN;

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:account:environment:env-id";

    private static final String ACCOUNT_ID = "account-id";

    private static final String ENV_NAME = "my-env";

    @InjectMocks
    private FreeIpaLoadBalancerDomainService underTest;

    @Mock
    private FreeIpaLoadBalancerService loadBalancerService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private FreeIpaLoadBalancerPemService freeIpaLoadBalancerPemService;

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @Mock
    private DnsPtrRecordService dnsPtrRecordService;

    @Mock
    private FreeIpaClient freeIpaClient;

    // -----------------------------------------------------------------------
    // Helper factories
    // -----------------------------------------------------------------------

    private LoadBalancer loadBalancer(String... ips) {
        LoadBalancer lb = new LoadBalancer();
        lb.setEndpoint(ENDPOINT);
        lb.setIp(Set.of(ips));
        return lb;
    }

    private FreeIpa freeIpa(String cloudPlatform) {
        Stack stack = new Stack();
        stack.setCloudPlatform(cloudPlatform);
        stack.setEnvironmentCrn(ENV_CRN);
        stack.setAccountId(ACCOUNT_ID);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain(DOMAIN);
        freeIpa.setStack(stack);
        return freeIpa;
    }

    private DetailedEnvironmentResponse envResponse() {
        DetailedEnvironmentResponse response = new DetailedEnvironmentResponse();
        response.setName(ENV_NAME);
        return response;
    }

    // -----------------------------------------------------------------------
    // registerLbDomain tests
    // -----------------------------------------------------------------------

    @Test
    void registerLbDomainDoesNothingWhenNoLoadBalancerExists() throws Exception {
        when(loadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.empty());

        underTest.registerLbDomain(STACK_ID, freeIpaClient);

        verify(freeIpaClient, never()).addDnsARecord(any(), any(), any(), anyBoolean());
        verify(dnsPtrRecordService, never()).addDnsPtrRecord(any(), any());
        verify(freeIpaLoadBalancerPemService, never()).createOrUpdateDnsEntry(any(), any(), any());
    }

    @Test
    void registerLbDomainCreatesARecordAndPtrRecord() throws Exception {
        when(loadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.of(loadBalancer(IP)));
        when(freeIpaService.findByStackId(STACK_ID)).thenReturn(freeIpa("AWS"));
        when(environmentEndpoint.getByCrn(ENV_CRN)).thenReturn(envResponse());

        underTest.registerLbDomain(STACK_ID, freeIpaClient);

        verify(freeIpaClient).addDnsARecord(DOMAIN, ENDPOINT, IP, false);
        verify(dnsPtrRecordService).addDnsPtrRecord(any(AddDnsPtrRecordRequest.class), eq(ACCOUNT_ID));
        verify(freeIpaLoadBalancerPemService).createOrUpdateDnsEntry(any(), eq(ENV_NAME), eq(ACCOUNT_ID));
    }

    @Test
    void registerLbDomainSendsFullyQualifiedDomainNameInPtrRequest() throws Exception {
        when(loadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.of(loadBalancer(IP)));
        when(freeIpaService.findByStackId(STACK_ID)).thenReturn(freeIpa("AWS"));
        when(environmentEndpoint.getByCrn(ENV_CRN)).thenReturn(envResponse());

        underTest.registerLbDomain(STACK_ID, freeIpaClient);

        ArgumentCaptor<AddDnsPtrRecordRequest> captor = ArgumentCaptor.forClass(AddDnsPtrRecordRequest.class);
        verify(dnsPtrRecordService).addDnsPtrRecord(captor.capture(), eq(ACCOUNT_ID));
        AddDnsPtrRecordRequest request = captor.getValue();
        assertEquals(EXPECTED_FQDN, request.getFqdn());
        assertEquals(IP, request.getIp());
        assertEquals(ENV_CRN, request.getEnvironmentCrn());
    }

    @Test
    void registerLbDomainCreatesARecordAndPtrForEachIp() throws Exception {
        String ip1 = "10.0.0.5";
        String ip2 = "10.0.0.6";
        when(loadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.of(loadBalancer(ip1, ip2)));
        when(freeIpaService.findByStackId(STACK_ID)).thenReturn(freeIpa("AWS"));
        when(environmentEndpoint.getByCrn(ENV_CRN)).thenReturn(envResponse());

        underTest.registerLbDomain(STACK_ID, freeIpaClient);

        verify(freeIpaClient).addDnsARecord(DOMAIN, ENDPOINT, ip1, false);
        verify(freeIpaClient).addDnsARecord(DOMAIN, ENDPOINT, ip2, false);

        ArgumentCaptor<AddDnsPtrRecordRequest> captor = ArgumentCaptor.forClass(AddDnsPtrRecordRequest.class);
        // two PTR requests, one per IP
        verify(dnsPtrRecordService, org.mockito.Mockito.times(2)).addDnsPtrRecord(captor.capture(), eq(ACCOUNT_ID));
        Set<String> capturedIps = Set.of(captor.getAllValues().get(0).getIp(), captor.getAllValues().get(1).getIp());
        assertEquals(Set.of(ip1, ip2), capturedIps);
        captor.getAllValues().forEach(req -> assertEquals(EXPECTED_FQDN, req.getFqdn()));
    }

    @Test
    void registerLbDomainDeletesConflictingPtrAndRecreatesIt() throws Exception {
        when(loadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.of(loadBalancer(IP)));
        when(freeIpaService.findByStackId(STACK_ID)).thenReturn(freeIpa("AWS"));
        when(environmentEndpoint.getByCrn(ENV_CRN)).thenReturn(envResponse());
        doThrow(new DnsRecordConflictException("conflict"))
                .doNothing()
                .when(dnsPtrRecordService).addDnsPtrRecord(any(), anyString());

        underTest.registerLbDomain(STACK_ID, freeIpaClient);

        // delete the conflicting record, then add again
        verify(dnsPtrRecordService).deleteDnsPtrRecord(any(), eq(ACCOUNT_ID));
        verify(dnsPtrRecordService, org.mockito.Mockito.times(2)).addDnsPtrRecord(any(), eq(ACCOUNT_ID));
    }

    @Test
    void registerLbDomainConflictingPtrDeleteUsesCorrectEnvironmentCrn() throws Exception {
        when(loadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.of(loadBalancer(IP)));
        when(freeIpaService.findByStackId(STACK_ID)).thenReturn(freeIpa("AWS"));
        when(environmentEndpoint.getByCrn(ENV_CRN)).thenReturn(envResponse());
        doThrow(new DnsRecordConflictException("conflict"))
                .doNothing()
                .when(dnsPtrRecordService).addDnsPtrRecord(any(), anyString());

        underTest.registerLbDomain(STACK_ID, freeIpaClient);

        ArgumentCaptor<com.sequenceiq.freeipa.api.v1.dns.model.DeleteDnsPtrRecordRequest> captor =
                ArgumentCaptor.forClass(com.sequenceiq.freeipa.api.v1.dns.model.DeleteDnsPtrRecordRequest.class);
        verify(dnsPtrRecordService).deleteDnsPtrRecord(captor.capture(), eq(ACCOUNT_ID));
        assertEquals(ENV_CRN, captor.getValue().getEnvironmentCrn());
        assertEquals(IP, captor.getValue().getIp());
    }

    @Test
    void registerLbDomainSkipsPemUpdateForMockPlatform() throws Exception {
        when(loadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.of(loadBalancer(IP)));
        when(freeIpaService.findByStackId(STACK_ID)).thenReturn(freeIpa("MOCK"));

        underTest.registerLbDomain(STACK_ID, freeIpaClient);

        verify(freeIpaClient).addDnsARecord(DOMAIN, ENDPOINT, IP, false);
        verify(dnsPtrRecordService).addDnsPtrRecord(any(), eq(ACCOUNT_ID));
        verify(freeIpaLoadBalancerPemService, never()).createOrUpdateDnsEntry(any(), anyString(), anyString());
    }

    @Test
    void registerLbDomainPropagatesFreeIpaClientExceptionFromARecord() throws Exception {
        FreeIpaClientException error = new FreeIpaClientException("unexpected error");
        when(loadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.of(loadBalancer(IP)));
        when(freeIpaService.findByStackId(STACK_ID)).thenReturn(freeIpa("AWS"));
        when(freeIpaClient.addDnsARecord(anyString(), anyString(), anyString(), anyBoolean())).thenThrow(error);

        assertThrows(FreeIpaClientException.class, () -> underTest.registerLbDomain(STACK_ID, freeIpaClient));

        verify(dnsPtrRecordService, never()).addDnsPtrRecord(any(), any());
    }

    @Test
    void registerLbDomainPropagatesFreeIpaClientExceptionFromPtrCreation() throws Exception {
        FreeIpaClientException error = new FreeIpaClientException("PTR creation failed");
        when(loadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.of(loadBalancer(IP)));
        when(freeIpaService.findByStackId(STACK_ID)).thenReturn(freeIpa("MOCK"));
        doThrow(error).when(dnsPtrRecordService).addDnsPtrRecord(any(), anyString());

        assertThrows(FreeIpaClientException.class, () -> underTest.registerLbDomain(STACK_ID, freeIpaClient));
    }

    @Test
    void registerLbDomainPropagatesPemDnsEntryCreateOrUpdateException() throws Exception {
        when(loadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.of(loadBalancer(IP)));
        when(freeIpaService.findByStackId(STACK_ID)).thenReturn(freeIpa("AWS"));
        when(environmentEndpoint.getByCrn(ENV_CRN)).thenReturn(envResponse());
        doThrow(new PemDnsEntryCreateOrUpdateException("pem failed", null))
                .when(freeIpaLoadBalancerPemService).createOrUpdateDnsEntry(any(), eq(ENV_NAME), eq(ACCOUNT_ID));

        assertThrows(PemDnsEntryCreateOrUpdateException.class, () -> underTest.registerLbDomain(STACK_ID, freeIpaClient));
    }
}

