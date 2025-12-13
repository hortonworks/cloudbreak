package com.sequenceiq.cloudbreak.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto;
import com.sequenceiq.cloudbreak.PemDnsEntryCreateOrUpdateException;
import com.sequenceiq.cloudbreak.certificate.service.DnsManagementService;
import com.sequenceiq.cloudbreak.client.GrpcClusterDnsClient;

@ExtendWith(MockitoExtension.class)
class DnsManagementServiceTest {

    private static final String EXAMPLE_CLDR_DOMAIN = "envname.cldr.site";

    @Mock
    private GrpcClusterDnsClient grpcClusterDnsClient;

    @InjectMocks
    private DnsManagementService underTest;

    @Test
    void testGenerateManagedDomainWhenDNSClientFailsWithAnException() {
        when(grpcClusterDnsClient.generateManagedDomain(anyString(), any(), anyString(), any()))
                .thenThrow(new RuntimeException("Something went wrong"));

        assertThrows(RuntimeException.class, () -> underTest.generateManagedDomain("accountId", "anEnvironmentName"));
        verify(grpcClusterDnsClient).generateManagedDomain(anyString(), any(), anyString(), any());
    }

    @Test
    void testGenerateManagedDomainWhenDNSClientReturnsWithNull() {
        when(grpcClusterDnsClient.generateManagedDomain(anyString(), any(), anyString(), any()))
                .thenReturn(null);

        String result = underTest.generateManagedDomain("accountId", "anEnvironmentName");

        assertNull(result);
        verify(grpcClusterDnsClient).generateManagedDomain(anyString(), any(), anyString(), any());
    }

    @Test
    void testGenerateManagedDomainWhenDNSClientReturnsButDoesNotContainTheRequestedSubDomain() {
        PublicEndpointManagementProto.GenerateManagedDomainNamesResponse resp = getGenerateManagedDomainNamesResponse("someSubDomain");
        when(grpcClusterDnsClient.generateManagedDomain(anyString(), any(), anyString(), any()))
                .thenReturn(resp);

        String result = underTest.generateManagedDomain("accountId", "anEnvironmentName");

        assertNull(result);
        verify(grpcClusterDnsClient).generateManagedDomain(anyString(), any(), anyString(), any());
    }

    @Test
    void testGenerateManagedDomainWhenDNSClientReturnsButDoesContainTheRequestedWildCardSubDomain() {
        PublicEndpointManagementProto.GenerateManagedDomainNamesResponse resp = getGenerateManagedDomainNamesResponse("*");
        when(grpcClusterDnsClient.generateManagedDomain(anyString(), any(), anyString(), any()))
                .thenReturn(resp);

        String result = underTest.generateManagedDomain("accountId", "anEnvironmentName");

        assertNotNull(result);
        assertEquals(EXAMPLE_CLDR_DOMAIN, result);
        verify(grpcClusterDnsClient).generateManagedDomain(anyString(), any(), anyString(), any());
    }

    @Test
    void testGenerateManagedDomainWhenDNSClientReturnsButDoesContainTheRequestedDomainWithWildCardAsPrefixSubdomain() {
        PublicEndpointManagementProto.GenerateManagedDomainNamesResponse resp = PublicEndpointManagementProto.GenerateManagedDomainNamesResponse.newBuilder()
                .putDomains("*", "*." + EXAMPLE_CLDR_DOMAIN)
                .build();
        when(grpcClusterDnsClient.generateManagedDomain(anyString(), any(), anyString(), any()))
                .thenReturn(resp);

        String result = underTest.generateManagedDomain("accountId", "anEnvironmentName");

        assertNotNull(result);
        assertEquals(EXAMPLE_CLDR_DOMAIN, result);
        verify(grpcClusterDnsClient).generateManagedDomain(anyString(), any(), anyString(), any());
    }

    @Test
    void testCreateOrUpdateDnsEntryWithIpShouldNotThrowExceptionWhenAllDnsEntriesCouldBeRegistered() throws PemDnsEntryCreateOrUpdateException {
        PublicEndpointManagementProto.CreateDnsEntryResponse resp = PublicEndpointManagementProto.CreateDnsEntryResponse.newBuilder().build();
        List<String> ips = List.of("10.0.1.11", "10.1.1.21");
        when(grpcClusterDnsClient.createOrUpdateDnsEntryWithIp(eq("accountId"), eq("endpointName"), eq("environmentName"),
                eq(false), eq(ips), any())).thenReturn(resp);

        underTest.createOrUpdateDnsEntryWithIp("accountId", "endpointName", "environmentName", false, ips);

        verify(grpcClusterDnsClient, times(1))
                .createOrUpdateDnsEntryWithIp(eq("accountId"), eq("endpointName"), eq("environmentName"), eq(false), eq(ips), any());
    }

    @Test
    void testCreateOrUpdateDnsEntryWithIpShouldCreateConsoleDnsAppsDomain() throws PemDnsEntryCreateOrUpdateException {
        PublicEndpointManagementProto.CreateDnsEntryResponse resp = PublicEndpointManagementProto.CreateDnsEntryResponse.newBuilder().build();
        List<String> ips = List.of("10.0.1.11");
        when(grpcClusterDnsClient.createOrUpdateDnsEntryWithIp(eq("accountId"), eq("console-cdp.apps"), eq("environmentName"),
                eq(false), eq(ips), any())).thenReturn(resp);

        underTest.createOrUpdateDnsEntryWithIp("accountId", "console-cdp.apps", "environmentName", false, ips);

        verify(grpcClusterDnsClient, times(1))
                .createOrUpdateDnsEntryWithIp(eq("accountId"), eq("console-cdp.apps"), eq("environmentName"), eq(false), eq(ips), any());
    }

    @Test
    void testCreateOrUpdateDnsEntryWithIpShouldThrowExceptionWhenOneOfTheDnsEntriesCouldNotBeRegistered() {
        PublicEndpointManagementProto.CreateDnsEntryResponse resp = PublicEndpointManagementProto.CreateDnsEntryResponse.newBuilder().build();
        List<String> ips = List.of("10.0.1.11", "10.1.1.21");
        when(grpcClusterDnsClient.createOrUpdateDnsEntryWithIp(eq("accountId"), eq("endpointName"), eq("environmentName"), eq(false), eq(ips), any()))
                .thenThrow(new RuntimeException("Something really bad happened...."));

        PemDnsEntryCreateOrUpdateException actualException = assertThrows(PemDnsEntryCreateOrUpdateException.class,
                () -> underTest.createOrUpdateDnsEntryWithIp("accountId", "endpointName", "environmentName", false, ips));
        assertEquals(
                "Failed to create DNS entry with endpoint name: 'endpointName', environment name: 'environmentName' and IPs: '10.0.1.11,10.1.1.21'",
                actualException.getMessage());
    }

    @Test
    void testCreateOrUpdateDnsEntryWithCloudDnsShouldNotThrowExceptionWhenAllDnsEntriesCouldBeRegistered() throws PemDnsEntryCreateOrUpdateException {
        PublicEndpointManagementProto.CreateDnsEntryResponse resp = PublicEndpointManagementProto.CreateDnsEntryResponse.newBuilder().build();
        List<String> ips = List.of("10.0.1.11", "10.1.1.21");
        when(grpcClusterDnsClient.createOrUpdateDnsEntryWithCloudDns(eq("accountId"), eq("endpointName"), eq("environmentName"),
                eq("cloudFQDN"), eq("hostedZoneId"), any())).thenReturn(resp);

        underTest.createOrUpdateDnsEntryWithCloudDns("accountId", "endpointName", "environmentName", "cloudFQDN", "hostedZoneId");

        verify(grpcClusterDnsClient, times(1))
                .createOrUpdateDnsEntryWithCloudDns(eq("accountId"), eq("endpointName"), eq("environmentName"), eq("cloudFQDN"), eq("hostedZoneId"), any());
    }

    @Test
    void testCreateOrUpdateDnsEntryWithCloudDnsShouldThrowExceptionWhenOneOfTheDnsEntriesCouldNotBeRegistered() {
        PublicEndpointManagementProto.CreateDnsEntryResponse resp = PublicEndpointManagementProto.CreateDnsEntryResponse.newBuilder().build();
        List<String> ips = List.of("10.0.1.11", "10.1.1.21");
        when(grpcClusterDnsClient.createOrUpdateDnsEntryWithCloudDns(eq("accountId"), eq("endpointName"), eq("environmentName"),
                eq("cloudFQDN"), eq("hostedZoneId"), any())).thenThrow(new RuntimeException("Something really bad happened...."));

        PemDnsEntryCreateOrUpdateException actualException = assertThrows(PemDnsEntryCreateOrUpdateException.class,
                () -> underTest.createOrUpdateDnsEntryWithCloudDns("accountId", "endpointName", "environmentName", "cloudFQDN", "hostedZoneId"));
        assertEquals(
                "Failed to create DNS entry with endpoint name: 'endpointName', environment name: 'environmentName' and cloud DNS: 'cloudFQDN'",
                actualException.getMessage());
    }

    private PublicEndpointManagementProto.GenerateManagedDomainNamesResponse getGenerateManagedDomainNamesResponse(String subDomain) {
        return PublicEndpointManagementProto.GenerateManagedDomainNamesResponse.newBuilder()
                .putDomains(subDomain, EXAMPLE_CLDR_DOMAIN)
                .build();
    }
}
