package com.sequenceiq.cloudbreak.service.publicendpoint;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaClientService;
import com.sequenceiq.cloudbreak.service.loadbalancer.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsARecordRequest;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsCnameRecordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

public class FreeIPAEndpointManagementServiceTest {

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com";

    private static final String ENVIRONMENT_CRN = "crn:myEnvironment";

    private static final String LB_DNS = "loadbalancer.dns.com";

    private static final String LB_IP = "1.1.1.1";

    private static final String LB_ENDPOINT = "lb-endpoint";

    @Mock
    private DnsV1Endpoint dnsV1Endpoint;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Mock
    private Stack stack;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private EnvironmentBasedDomainNameProvider domainNameProvider;

    @Mock
    private LoadBalancerConfigService loadBalancerConfigService;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Mock
    private FreeipaClientService freeipaClientService;

    @InjectMocks
    private FreeIPAEndpointManagementService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        stack = mock(Stack.class);
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(stack.getId()).thenReturn(1L);
        doNothing().when(dnsV1Endpoint).addDnsCnameRecordInternal(any(), any());
        doNothing().when(dnsV1Endpoint).addDnsARecordInternal(any(), any());
        doNothing().when(dnsV1Endpoint).deleteDnsCnameRecord(any(), any(), any());
        doNothing().when(dnsV1Endpoint).deleteDnsARecord(any(), any(), any());

        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setName("environment-name");
        when(environmentClientService.getByCrn(any())).thenReturn(environment);

        String accountWorkloadSubdomain = "aWorkloadSubdomain";
        UserManagementProto.Account umsAccount = UserManagementProto.Account.newBuilder()
            .setWorkloadSubdomain(accountWorkloadSubdomain)
            .build();
        when(grpcUmsClient.getAccountDetails(any())).thenReturn(umsAccount);

        underTest.setCertGenerationEnabled(true);
    }

    @Test
    public void testRegisterLoadBalancerWithDNS() {
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setType(LoadBalancerType.PUBLIC);
        loadBalancer.setDns(LB_DNS);
        loadBalancer.setEndpoint(LB_ENDPOINT);
        when(loadBalancerPersistenceService.findByStackId(any())).thenReturn(Set.of(loadBalancer));
        when(loadBalancerConfigService.selectLoadBalancerForFrontend(any(), any())).thenReturn(Optional.of(loadBalancer));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(freeipaClientService.findByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Optional.of(new DescribeFreeIpaResponse()));

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.registerLoadBalancerDomainWithFreeIPA(stack));

        ArgumentCaptor<AddDnsCnameRecordRequest> requestCaptor = ArgumentCaptor.forClass(AddDnsCnameRecordRequest.class);
        verify(dnsV1Endpoint, times(1)).addDnsCnameRecordInternal(any(), requestCaptor.capture());
        assertEquals(LB_DNS + '.', requestCaptor.getValue().getTargetFqdn());
        assertEquals(LB_ENDPOINT, requestCaptor.getValue().getCname());
    }

    @Test
    public void testRegisterLoadBalancerWithIP() {
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setType(LoadBalancerType.PUBLIC);
        loadBalancer.setIp(LB_IP);
        loadBalancer.setEndpoint(LB_ENDPOINT);
        when(loadBalancerPersistenceService.findByStackId(any())).thenReturn(Set.of(loadBalancer));
        when(loadBalancerConfigService.selectLoadBalancerForFrontend(any(), any())).thenReturn(Optional.of(loadBalancer));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(freeipaClientService.findByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Optional.of(new DescribeFreeIpaResponse()));

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.registerLoadBalancerDomainWithFreeIPA(stack));

        ArgumentCaptor<AddDnsARecordRequest> requestCaptor = ArgumentCaptor.forClass(AddDnsARecordRequest.class);
        verify(dnsV1Endpoint, times(1)).addDnsARecordInternal(any(), requestCaptor.capture());
        assertEquals(LB_IP, requestCaptor.getValue().getIp());
        assertEquals(LB_ENDPOINT, requestCaptor.getValue().getHostname());
    }

    @Test
    public void testDeleteLoadBalancerDomainWithDNS() {
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setType(LoadBalancerType.PUBLIC);
        loadBalancer.setDns(LB_DNS);
        loadBalancer.setEndpoint(LB_ENDPOINT);
        when(loadBalancerPersistenceService.findByStackId(any())).thenReturn(Set.of(loadBalancer));
        when(loadBalancerConfigService.selectLoadBalancerForFrontend(any(), any())).thenReturn(Optional.of(loadBalancer));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(freeipaClientService.findByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Optional.of(new DescribeFreeIpaResponse()));

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.deleteLoadBalancerDomainFromFreeIPA(stack));

        verify(dnsV1Endpoint, times(1)).deleteDnsCnameRecord(any(), any(), eq(loadBalancer.getEndpoint()));
    }

    @Test
    public void testDeleteLoadBalancerDomainWithIP() {
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setType(LoadBalancerType.PUBLIC);
        loadBalancer.setIp(LB_IP);
        loadBalancer.setEndpoint(LB_ENDPOINT);
        when(loadBalancerPersistenceService.findByStackId(any())).thenReturn(Set.of(loadBalancer));
        when(loadBalancerConfigService.selectLoadBalancerForFrontend(any(), any())).thenReturn(Optional.of(loadBalancer));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(freeipaClientService.findByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(Optional.of(new DescribeFreeIpaResponse()));

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.deleteLoadBalancerDomainFromFreeIPA(stack));

        verify(dnsV1Endpoint, times(1)).deleteDnsARecord(any(), any(), eq(loadBalancer.getEndpoint()));
    }
}
