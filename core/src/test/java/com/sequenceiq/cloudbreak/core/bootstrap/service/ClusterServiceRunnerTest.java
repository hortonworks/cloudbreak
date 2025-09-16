package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class ClusterServiceRunnerTest {

    private static final String IP_1 = "1.2.3.4";

    private static final String IP_2 = "4.3.2.1";

    @Mock
    private StackDto stackDto;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ClusterHostServiceRunner hostRunner;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @InjectMocks
    private ClusterServiceRunner underTest;

    @Test
    void testRedeployGatewayCertificateWhenRedeployOnHostRunnerThrowRuntimeException() {
        when(stackDtoService.getById(anyLong())).thenReturn(stackDto);
        doThrow(new RuntimeException("Stg. failed")).when(hostRunner).redeployGatewayCertificate(any());

        Assertions.assertThrows(RuntimeException.class, () -> underTest.redeployGatewayCertificate(0L));
    }

    @Test
    void testRedeployGatewayCertificateHappyFlow() {
        when(stackDtoService.getById(anyLong())).thenReturn(stackDto);

        underTest.redeployGatewayCertificate(0L);

        verify(hostRunner, times(1)).redeployGatewayCertificate(any());
    }

    @Test
    void testRedeployGatewayPillar() {
        when(stackDtoService.getById(anyLong())).thenReturn(stackDto);
        underTest.redeployGatewayPillar(1L);
        verify(hostRunner).redeployGatewayPillarOnly(stackDto, Set.of());
    }

    @Test
    void testRedeployStates() {
        when(stackDtoService.getById(anyLong())).thenReturn(stackDto);
        underTest.redeployStates(1L);
        verify(hostRunner).redeployStates(stackDto);
    }

    @Test
    void testUpdateAmbariClientConfigIpNotChanged() {
        when(gatewayConfigService.getPrimaryGatewayIp(stackDto)).thenReturn(IP_1);
        when(stackDto.getClusterManagerIp()).thenReturn(IP_1);

        String result = underTest.updateClusterManagerClientConfig(stackDto);

        assertEquals(IP_1, result);
        verifyNoInteractions(tlsSecurityService);
        verifyNoInteractions(clusterService);
    }

    @Test
    void testUpdateAmbariClientConfigIpChanged() {
        when(gatewayConfigService.getPrimaryGatewayIp(stackDto)).thenReturn(IP_2);
        when(stackDto.getClusterManagerIp()).thenReturn(IP_1);
        StackView stack = mock(StackView.class);
        when(stackDto.getStack()).thenReturn(stack);
        when(stack.getId()).thenReturn(42L);
        when(stack.getCloudPlatform()).thenReturn("platform");
        HttpClientConfig config = mock(HttpClientConfig.class);
        when(tlsSecurityService.buildTLSClientConfigForPrimaryGateway(42L, IP_2, "platform")).thenReturn(config);
        ClusterView cluster = mock(ClusterView.class);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(cluster.getId()).thenReturn(24L);

        String result = underTest.updateClusterManagerClientConfig(stackDto);

        assertEquals(IP_2, result);
        verify(tlsSecurityService).buildTLSClientConfigForPrimaryGateway(42L, IP_2, "platform");
        verify(clusterService).updateClusterManagerClientConfig(24L, config);
    }
}
