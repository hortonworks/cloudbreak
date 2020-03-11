package com.sequenceiq.cloudbreak.service.publicendpoint;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.stack.Stack;

@ExtendWith(MockitoExtension.class)
class ClusterPublicEndpointManagementServiceTest {

    @Mock
    private Stack stack;

    @Mock
    private GatewayPublicEndpointManagementService gatewayPublicEndpointManagementService;

    @Mock
    private KafkaBrokerPublicDnsEntryService kafkaBrokerPublicDnsEntryService;

    @InjectMocks
    private ClusterPublicEndpointManagementService underTest;

    @Test
    void testStartWhenPemIsDisabled() {
        when(gatewayPublicEndpointManagementService.manageCertificateAndDnsInPem()).thenReturn(false);

        underTest.start(stack);

        verifyZeroInteractions(gatewayPublicEndpointManagementService, kafkaBrokerPublicDnsEntryService);
    }

    @Test
    void testStartWhenPemIsEnabled() {
        when(gatewayPublicEndpointManagementService.manageCertificateAndDnsInPem()).thenReturn(true);

        underTest.start(stack);

        verify(gatewayPublicEndpointManagementService, times(1)).updateDnsEntry(stack, null);
        verify(kafkaBrokerPublicDnsEntryService, times(1)).createOrUpdate(stack);
    }
}