package com.sequenceiq.cloudbreak.core.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.image.ClusterUpgradeTargetImageService;
import com.sequenceiq.cloudbreak.service.upgrade.image.OsChangeService;
import com.sequenceiq.cloudbreak.util.NodesUnreachableException;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class ClusterManagerUpgradeServiceTest {

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private OsChangeService osChangeService;

    @Mock
    private ClusterUpgradeTargetImageService clusterUpgradeTargetImageService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ClouderaManagerRepo clouderaManagerRepo;

    @InjectMocks
    private ClusterManagerUpgradeService underTest;

    @Spy
    private StackDto stackDto;

    private Stack stack;

    private Cluster cluster;

    @BeforeEach
    public void setUp() throws NodesUnreachableException {
        stack = TestUtil.stack(Status.AVAILABLE, TestUtil.awsCredential());
        cluster = TestUtil.cluster();
        when(stackDto.hasGateway()).thenReturn(cluster.getGateway() != null);
        when(stackDto.getPrimaryGatewayInstance()).thenReturn(stack.getPrimaryGatewayInstance());
        when(stackDto.getStack()).thenReturn(stack);
        when(stackDto.getSecurityConfig()).thenReturn(stack.getSecurityConfig());
    }

    @Test
    void testUpgradeClouderaManagerShouldCallTheUpgradeCommand() throws CloudbreakOrchestratorException, NodesUnreachableException {
        when(stackUtil.collectNodes(stackDto)).thenReturn(Collections.emptySet());
        when(stackUtil.collectReachableAndCheckNecessaryNodes(stackDto, Collections.emptySet())).thenReturn(Collections.emptySet());
        when(clusterUpgradeTargetImageService.findTargetImage(stack.getId()))
                .thenReturn(Optional.of(Image.builder().withOsType(OsType.RHEL9.getOsType()).withArchitecture(Architecture.X86_64.getName()).build()));

        underTest.upgradeClouderaManager(stackDto, clouderaManagerRepo, OsType.RHEL8);

        verify(gatewayConfigService, times(1)).getGatewayConfig(stack, stack.getSecurityConfig(), stack.getPrimaryGatewayInstance(),
                cluster.getGateway() != null);
        verify(hostOrchestrator, times(1)).upgradeClusterManager(any(), any(), any(), any(), any());
        verify(clusterHostServiceRunner, times(1)).decoratePillarWithClouderaManagerRepo(any(), any(), any());
        verify(clusterHostServiceRunner, times(1)).createPillarWithClouderaManagerSettings(any(), any(), any());
        verify(osChangeService).updateCmRepoInCaseOfOsChange(clouderaManagerRepo, OsType.RHEL8, OsType.RHEL9, Architecture.X86_64.getName());
    }

    @Test
    void testUpgradeClouderaManagerShouldThrowExceptionWhenImageNotFound() {
        when(clusterUpgradeTargetImageService.findTargetImage(stack.getId())).thenReturn(Optional.empty());

        String errorMessage = assertThrows(CloudbreakRuntimeException.class,
                () -> underTest.upgradeClouderaManager(stackDto, clouderaManagerRepo, OsType.RHEL8)).getMessage();

        assertEquals("Target image not found", errorMessage);
        verify(gatewayConfigService, times(1)).getGatewayConfig(stack, stack.getSecurityConfig(), stack.getPrimaryGatewayInstance(),
                cluster.getGateway() != null);
    }

}