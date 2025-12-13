package com.sequenceiq.cloudbreak.service.publicendpoint;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.publicendpoint.dns.AllHostPublicDnsEntryService;
import com.sequenceiq.cloudbreak.service.publicendpoint.dns.BaseDnsEntryService;
import com.sequenceiq.cloudbreak.service.publicendpoint.dns.KafkaBrokerPublicDnsEntryService;

@ExtendWith(MockitoExtension.class)
class ClusterPublicEndpointManagementServiceTest {

    @Mock
    private Stack stack;

    @Mock
    private GatewayPublicEndpointManagementService gatewayPublicEndpointManagementService;

    @Mock
    private List<BaseDnsEntryService> dnsEntryServices;

    @InjectMocks
    private ClusterPublicEndpointManagementService underTest;

    @Test
    void testRefreshDnsEntriesWhenPemIsDisabled() {
        when(gatewayPublicEndpointManagementService.manageCertificateAndDnsInPem(any())).thenReturn(false);

        underTest.refreshDnsEntries(stack);

        verify(gatewayPublicEndpointManagementService, never()).updateDnsEntry(stack, null);
        verifyNoInteractions(dnsEntryServices);
    }

    @Test
    void testRefreshDnsEntriesWhenPemIsEnabled() {
        when(gatewayPublicEndpointManagementService.manageCertificateAndDnsInPem(any())).thenReturn(true);

        underTest.refreshDnsEntries(stack);

        verify(gatewayPublicEndpointManagementService, times(1)).updateDnsEntry(stack, null);
        verify(dnsEntryServices, times(1)).forEach(any());
    }

    @Test
    void testChangeGateway() {
        when(gatewayPublicEndpointManagementService.manageCertificateAndDnsInPem(any())).thenReturn(true);
        underTest.changeGateway(stack);
        verify(gatewayPublicEndpointManagementService, times(1)).updateDnsEntryForCluster(stack);
    }

    @Test
    void testUpscale() {
        InstanceMetaData pgw = new InstanceMetaData();
        pgw.setPublicIp("192.168.1.1");
        when(stack.getPrimaryGatewayInstance()).thenReturn(pgw);
        when(gatewayPublicEndpointManagementService.manageCertificateAndDnsInPem(any())).thenReturn(true);
        underTest.upscale(stack, Map.of("fqdn1", "192.168.1.1", "fqdn2", "192.168.1.2"));
        verify(gatewayPublicEndpointManagementService, times(1)).updateDnsEntryForCluster(stack);
        verify(dnsEntryServices, times(1)).forEach(any());
    }

    @Test
    void testProvision() {
        KafkaBrokerPublicDnsEntryService kafkaBrokerPublicDnsEntryService = mock(KafkaBrokerPublicDnsEntryService.class);
        AllHostPublicDnsEntryService allHostPublicDnsEntryService = mock(AllHostPublicDnsEntryService.class);
        when(dnsEntryServices.iterator()).thenReturn(List.of(kafkaBrokerPublicDnsEntryService, allHostPublicDnsEntryService).iterator());
        doCallRealMethod().when(dnsEntryServices).forEach(any());

        underTest.provision(stack);

        verify(gatewayPublicEndpointManagementService, times(1)).generateCertAndSaveForStackAndUpdateDnsEntry(stack);
        verify(kafkaBrokerPublicDnsEntryService, times(1)).createOrUpdate(stack);
        verify(allHostPublicDnsEntryService, times(1)).createOrUpdate(stack);
    }

    @Test
    void testProvisionWhenCertGenerationFails() {
        doThrow(new CloudbreakServiceException("Uh-Oh")).when(gatewayPublicEndpointManagementService).generateCertAndSaveForStackAndUpdateDnsEntry(stack);

        assertThrows(CloudbreakServiceException.class, () -> underTest.provision(stack));

        verify(gatewayPublicEndpointManagementService, times(1)).generateCertAndSaveForStackAndUpdateDnsEntry(stack);
        verifyNoInteractions(dnsEntryServices);
    }

    @Test
    void testProvisionWhenOneOfTheDnsEntryServicesFails() {
        KafkaBrokerPublicDnsEntryService kafkaBrokerPublicDnsEntryService = mock(KafkaBrokerPublicDnsEntryService.class);
        AllHostPublicDnsEntryService allHostPublicDnsEntryService = mock(AllHostPublicDnsEntryService.class);
        when(dnsEntryServices.iterator()).thenReturn(List.of(kafkaBrokerPublicDnsEntryService, allHostPublicDnsEntryService).iterator());
        doCallRealMethod().when(dnsEntryServices).forEach(any());
        doThrow(new CloudbreakServiceException("Uh-Oh")).when(allHostPublicDnsEntryService).createOrUpdate(stack);

        assertThrows(CloudbreakServiceException.class, () -> underTest.provision(stack));

        verify(gatewayPublicEndpointManagementService, times(1)).generateCertAndSaveForStackAndUpdateDnsEntry(stack);
        verify(allHostPublicDnsEntryService, times(1)).createOrUpdate(stack);
    }

    @Test
    void testProvisionLoadBalancer() {
        underTest.provisionLoadBalancer(stack);

        verify(gatewayPublicEndpointManagementService, times(1)).renewCertificate(stack);
        verify(gatewayPublicEndpointManagementService, times(1)).updateDnsEntryForLoadBalancers(stack);
    }

    @Test
    void testProvisionLoadBalancerWhenRenewingCertificateFails() {
        doThrow(new CloudbreakServiceException("Uh-Oh")).when(gatewayPublicEndpointManagementService).renewCertificate(stack);

        assertThrows(CloudbreakServiceException.class, () -> underTest.provisionLoadBalancer(stack));

        verify(gatewayPublicEndpointManagementService, times(1)).renewCertificate(stack);
    }

    @Test
    void testProvisionLoadBalancerWhenUpdatingDnsEntryForLoadBalancerFails() {
        doThrow(new CloudbreakServiceException("Uh-Oh")).when(gatewayPublicEndpointManagementService).updateDnsEntryForLoadBalancers(stack);

        assertThrows(CloudbreakServiceException.class, () -> underTest.provisionLoadBalancer(stack));

        verify(gatewayPublicEndpointManagementService, times(1)).renewCertificate(stack);
        verify(gatewayPublicEndpointManagementService, times(1)).updateDnsEntryForLoadBalancers(stack);
    }
}