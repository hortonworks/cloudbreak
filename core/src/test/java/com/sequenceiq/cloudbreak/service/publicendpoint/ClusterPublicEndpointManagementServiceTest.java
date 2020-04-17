package com.sequenceiq.cloudbreak.service.publicendpoint;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.publicendpoint.dns.BaseDnsEntryService;

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
    void testStartWhenPemIsDisabled() {
        when(gatewayPublicEndpointManagementService.manageCertificateAndDnsInPem()).thenReturn(false);

        underTest.start(stack);

        verifyZeroInteractions(gatewayPublicEndpointManagementService, dnsEntryServices);
    }

    @Test
    void testStartWhenPemIsEnabled() {
        when(gatewayPublicEndpointManagementService.manageCertificateAndDnsInPem()).thenReturn(true);

        underTest.start(stack);

        verify(gatewayPublicEndpointManagementService, times(1)).updateDnsEntry(stack, null);
        verify(dnsEntryServices, times(1)).forEach(any());
    }
}