package com.sequenceiq.cloudbreak.core.cluster;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class ClusterManagerRestartServiceTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private ClusterManagerRestartService underTest;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private ClusterManagerDefaultConfigAdjuster clusterManagerDefaultConfigAdjuster;

    @Test
    public void testRestartClouderaManager() throws CloudbreakOrchestratorException, ClusterClientInitException, CloudbreakException {
        StackDto stackDto = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        ClusterView clusterView = mock(ClusterView.class);
        InstanceMetadataView instanceMetadataView = mock(InstanceMetadataView.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        SecurityConfig securityConfig = new SecurityConfig();
        when(stackView.getId()).thenReturn(STACK_ID);
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackDto.getSecurityConfig()).thenReturn(securityConfig);
        when(stackDto.hasGateway()).thenReturn(true);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getId()).thenReturn(STACK_ID);

        when(stackDto.getPrimaryGatewayInstance()).thenReturn(instanceMetadataView);
        when(instanceMetadataView.getDiscoveryFQDN()).thenReturn("fqdn");
        when(gatewayConfigService.getGatewayConfig(stackView, securityConfig, instanceMetadataView, true)).thenReturn(gatewayConfig);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);

        underTest.restartClouderaManager(STACK_ID);

        verify(hostOrchestrator).restartClusterManagerOnMaster(eq(gatewayConfig), anySet(), any());
        verify(clusterManagerDefaultConfigAdjuster).waitForClusterManagerToBecomeAvailable(stackDto, false);
    }

    @Test
    public void testRestartClouderaManagerShouldThrowException() throws CloudbreakOrchestratorException {
        StackDto stackDto = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        ClusterView clusterView = mock(ClusterView.class);
        InstanceMetadataView instanceMetadataView = mock(InstanceMetadataView.class);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        SecurityConfig securityConfig = new SecurityConfig();
        when(stackView.getId()).thenReturn(STACK_ID);
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackDto.getSecurityConfig()).thenReturn(securityConfig);
        when(stackDto.hasGateway()).thenReturn(true);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getId()).thenReturn(STACK_ID);

        when(stackDto.getPrimaryGatewayInstance()).thenReturn(instanceMetadataView);
        when(instanceMetadataView.getDiscoveryFQDN()).thenReturn("fqdn");
        when(gatewayConfigService.getGatewayConfig(stackView, securityConfig, instanceMetadataView, true)).thenReturn(gatewayConfig);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);

        doThrow(new CloudbreakOrchestratorFailedException("Failed")).when(hostOrchestrator).restartClusterManagerOnMaster(eq(gatewayConfig), anySet(), any());

        assertThrows(CloudbreakServiceException.class, () -> underTest.restartClouderaManager(STACK_ID));

        verify(hostOrchestrator).restartClusterManagerOnMaster(eq(gatewayConfig), anySet(), any());
    }

}