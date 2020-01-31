package com.sequenceiq.cloudbreak.service.publicendpoint;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.certificate.service.DnsManagementService;
import com.sequenceiq.cloudbreak.dns.EnvironmentBasedDomainNameProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class GatewayPublicEndpointManagementServiceTest {
    private static final String USER_CRN = "crn:cdp:iam:us-west-1:123:user:123";

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private DnsManagementService dnsManagementService;

    @Mock
    private EnvironmentBasedDomainNameProvider domainNameProvider;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @InjectMocks
    private GatewayPublicEndpointManagementService underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "certGenerationEnabled", Boolean.TRUE);
    }

    @Test
    void testUpdateDnsEntryShouldCallDnsManagementServiceWithGatewayInstanceShortHostnameAsEndpointName() {
        String gatewayIp = "10.191.192.193";
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder
                .builder()
                .withName(envName)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        when(dnsManagementService.createDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(gatewayIp)))).thenReturn(Boolean.TRUE);
        String accountWorkloadSubdomain = "aWorkloadSubdomain";
        UserManagementProto.Account umsAccount = UserManagementProto.Account.newBuilder()
                .setWorkloadSubdomain(accountWorkloadSubdomain)
                .build();
        when(grpcUmsClient.getAccountDetails(USER_CRN, "123", Optional.empty())).thenReturn(umsAccount);
        String fqdn = endpointName + ".anenvname.xcu2-8y8x.dev.cldr.work";
        when(domainNameProvider.getFullyQualifiedEndpointName(endpointName, envName, accountWorkloadSubdomain)).thenReturn(fqdn);

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateDnsEntry(stack, gatewayIp));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(fqdn, result);
        verify(dnsManagementService, times(1))
                .createDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(gatewayIp)));
    }

    @Test
    void testDeleteDnsEntryShouldCallDnsManagementServiceWithGatewayInstanceShortHostnameAsEndpointNameWhenEnvNameIsSpecified() {
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String gatewayIp = primaryGatewayInstance.getPublicIpWrapper();
        String envName = "anEnvName";
        when(dnsManagementService.deleteDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(gatewayIp)))).thenReturn(Boolean.TRUE);

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.deleteDnsEntry(stack, envName));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(gatewayIp, result);
        verify(dnsManagementService, times(1))
                .deleteDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(gatewayIp)));
    }

    @Test
    void testDeleteDnsEntryShouldCallDnsManagementServiceWithGatewayInstanceShortHostnameAsEndpointNameWhenEnvNameIsNotSpecified() {
        Cluster cluster = TestUtil.cluster();
        Stack stack = cluster.getStack();
        stack.setCluster(cluster);
        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String endpointName = primaryGatewayInstance.getShortHostname();
        String gatewayIp = primaryGatewayInstance.getPublicIpWrapper();
        String envName = "anEnvName";
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.Builder
                .builder()
                .withName(envName)
                .build();
        when(environmentClientService.getByCrn(Mockito.anyString())).thenReturn(environment);
        when(dnsManagementService.deleteDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                eq(List.of(gatewayIp)))).thenReturn(Boolean.TRUE);

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.deleteDnsEntry(stack, null));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(gatewayIp, result);
        verify(dnsManagementService, times(1))
                .deleteDnsEntryWithIp(eq(USER_CRN), eq("123"), eq(endpointName), eq(envName), eq(Boolean.FALSE),
                        eq(List.of(gatewayIp)));
    }
}