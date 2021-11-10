package com.sequenceiq.cloudbreak.component;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto;
import com.sequenceiq.cloudbreak.certificate.service.DnsManagementService;
import com.sequenceiq.cloudbreak.client.GrpcClusterDnsClient;

@ExtendWith(MockitoExtension.class)
public class DnsManagementServiceTest {

    private static final String EXAMPLE_CLDR_DOMAIN = "envname.cldr.site";

    @Mock
    private GrpcClusterDnsClient grpcClusterDnsClient;

    @InjectMocks
    private DnsManagementService underTest;

    @Test
    void testGenerateManagedDomainWhenDNSClientFailsWithAnException() {
        when(grpcClusterDnsClient.generateManagedDomain(anyString(), any(), anyString(), any()))
                .thenThrow(new RuntimeException("Something went wrong"));

        Assertions.assertThrows(RuntimeException.class, () -> underTest.generateManagedDomain("accountId", "anEnvironmentName"));
        verify(grpcClusterDnsClient).generateManagedDomain(anyString(), any(), anyString(), any());
    }

    @Test
    void testGenerateManagedDomainWhenDNSClientReturnsWithNull() {
        when(grpcClusterDnsClient.generateManagedDomain(anyString(), any(), anyString(), any()))
                .thenReturn(null);

        String result = underTest.generateManagedDomain("accountId", "anEnvironmentName");

        Assertions.assertNull(result);
        verify(grpcClusterDnsClient).generateManagedDomain(anyString(), any(), anyString(), any());
    }

    @Test
    void testGenerateManagedDomainWhenDNSClientReturnsButDoesNotContainTheRequestedSubDomain() {
        PublicEndpointManagementProto.GenerateManagedDomainNamesResponse resp = getGenerateManagedDomainNamesResponse("someSubDomain");
        when(grpcClusterDnsClient.generateManagedDomain(anyString(), any(), anyString(), any()))
                .thenReturn(resp);

        String result = underTest.generateManagedDomain("accountId", "anEnvironmentName");

        Assertions.assertNull(result);
        verify(grpcClusterDnsClient).generateManagedDomain(anyString(), any(), anyString(), any());
    }

    @Test
    void testGenerateManagedDomainWhenDNSClientReturnsButDoesContainTheRequestedWildCardSubDomain() {
        PublicEndpointManagementProto.GenerateManagedDomainNamesResponse resp = getGenerateManagedDomainNamesResponse("*");
        when(grpcClusterDnsClient.generateManagedDomain(anyString(), any(), anyString(), any()))
                .thenReturn(resp);

        String result = underTest.generateManagedDomain("accountId", "anEnvironmentName");

        Assertions.assertNotNull(result);
        Assertions.assertEquals(EXAMPLE_CLDR_DOMAIN, result);
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

        Assertions.assertNotNull(result);
        Assertions.assertEquals(EXAMPLE_CLDR_DOMAIN, result);
        verify(grpcClusterDnsClient).generateManagedDomain(anyString(), any(), anyString(), any());
    }

    private PublicEndpointManagementProto.GenerateManagedDomainNamesResponse getGenerateManagedDomainNamesResponse(String subDomain) {
        return PublicEndpointManagementProto.GenerateManagedDomainNamesResponse.newBuilder()
                .putDomains(subDomain, EXAMPLE_CLDR_DOMAIN)
                .build();
    }
}
