package com.sequenceiq.cloudbreak.core.cluster;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.CsdParcelDecorator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.CmServerQueryService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
public class ClusterManagerUpgradeServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String CM_VERSION = "7.2.6-12345";

    private static final String CM_VERSION_WITH_P = "7.2.6-12345p";

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private CsdParcelDecorator csdParcelDecorator;

    @Mock
    private CmServerQueryService cmServerQueryService;

    @InjectMocks
    private ClusterManagerUpgradeService underTest;

    @Spy
    private StackDto stackDto;

    private Stack stack;

    private Cluster cluster;

    @BeforeEach
    public void setUp() {
        stack = TestUtil.stack(Status.AVAILABLE, TestUtil.awsCredential());
        cluster = TestUtil.cluster();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(stackDto.hasGateway()).thenReturn(cluster.getGateway() != null);
        when(stackDto.getPrimaryGatewayInstance()).thenReturn(stack.getPrimaryGatewayInstance());
        when(stackDto.getStack()).thenReturn(stack);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(stackDto.getSecurityConfig()).thenReturn(stack.getSecurityConfig());
        when(cmServerQueryService.queryCmVersion(stackDto)).thenReturn(Optional.empty());
    }

    private static Stream<Arguments> cmVersions() {
        return Stream.of(
                Arguments.of(CM_VERSION, CM_VERSION),
                Arguments.of(CM_VERSION_WITH_P, CM_VERSION),
                Arguments.of(CM_VERSION, CM_VERSION_WITH_P),
                Arguments.of(CM_VERSION_WITH_P, CM_VERSION_WITH_P)
        );
    }

    @ParameterizedTest
    @MethodSource("cmVersions")
    public void testUpgradeClusterManager(String versionOnHost, String versionInRepo) throws CloudbreakOrchestratorException, CloudbreakException {
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        when(clouderaManagerRepo.getFullVersion()).thenReturn(versionInRepo);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(cluster.getId())).thenReturn(clouderaManagerRepo);
        when(cmServerQueryService.queryCmVersion(stackDto)).thenReturn(Optional.of(versionOnHost));

        underTest.upgradeClusterManager(STACK_ID, true);

        verify(gatewayConfigService, times(1)).getGatewayConfig(stack, stack.getSecurityConfig(), stack.getPrimaryGatewayInstance(),
                cluster.getGateway() != null);
        verify(hostOrchestrator, times(1)).upgradeClusterManager(any(), any(), any(), any(), any());
        verify(clusterApi).stopCluster(true);
        verify(clusterHostServiceRunner, times(1)).decoratePillarWithClouderaManagerRepo(any(), any(), any());
        verify(clusterHostServiceRunner, times(1)).createPillarWithClouderaManagerSettings(any(), any(), any());
        verify(clusterApi).startCluster();
    }

    @Test
    public void testUpgradeClusterManagerVersionIsDifferent() throws CloudbreakOrchestratorException, CloudbreakException {
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        when(clouderaManagerRepo.getFullVersion()).thenReturn(CM_VERSION);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(cluster.getId())).thenReturn(clouderaManagerRepo);
        when(cmServerQueryService.queryCmVersion(stackDto)).thenReturn(Optional.of("wrong"));

        assertThrows(CloudbreakServiceException.class, () -> underTest.upgradeClusterManager(STACK_ID, true));

        verify(gatewayConfigService, times(1)).getGatewayConfig(stack, stack.getSecurityConfig(), stack.getPrimaryGatewayInstance(),
                cluster.getGateway() != null);
        verify(hostOrchestrator, times(1)).upgradeClusterManager(any(), any(), any(), any(), any());
        verify(clusterApi).stopCluster(true);
        verify(clusterHostServiceRunner, times(1)).decoratePillarWithClouderaManagerRepo(any(), any(), any());
        verify(clusterHostServiceRunner, times(1)).createPillarWithClouderaManagerSettings(any(), any(), any());
        verify(clusterApi).startCluster();
    }

    @Test
    public void testUpgradeClusterManagerWithoutStartServices() throws CloudbreakOrchestratorException, CloudbreakException {
        underTest.upgradeClusterManager(STACK_ID, false);

        verify(gatewayConfigService, times(1)).getGatewayConfig(stack, stack.getSecurityConfig(), stack.getPrimaryGatewayInstance(),
                cluster.getGateway() != null);
        verify(clusterComponentConfigProvider, times(1)).getClouderaManagerRepoDetails(cluster.getId());
        verify(hostOrchestrator, times(1)).upgradeClusterManager(any(), any(), any(), any(), any());
        verify(clusterApi).stopCluster(true);
        verify(clusterHostServiceRunner, times(1)).decoratePillarWithClouderaManagerRepo(any(), any(), any());
        verify(clusterHostServiceRunner, times(1)).createPillarWithClouderaManagerSettings(any(), any(), any());
        verify(clusterApi, never()).startCluster();
    }

    @Test
    public void testUpgradeClusterManagerShouldNotAddCsdToPillarWhenTheClusterTypeIsDataLake() throws CloudbreakOrchestratorException, CloudbreakException {
        stack.setType(StackType.DATALAKE);

        underTest.upgradeClusterManager(STACK_ID, true);

        verify(gatewayConfigService, times(1)).getGatewayConfig(stack, stack.getSecurityConfig(), stack.getPrimaryGatewayInstance(),
                cluster.getGateway() != null);
        verify(clusterComponentConfigProvider, times(1)).getClouderaManagerRepoDetails(cluster.getId());
        verify(hostOrchestrator, times(1)).upgradeClusterManager(any(), any(), any(), any(), any());
        verify(clusterApi).stopCluster(true);
        verify(clusterHostServiceRunner, times(1)).decoratePillarWithClouderaManagerRepo(any(), any(), any());
        verify(clusterHostServiceRunner, times(1)).createPillarWithClouderaManagerSettings(any(), any(), any());
        verify(clusterApi).startCluster();
        verify(csdParcelDecorator, times(1)).decoratePillarWithCsdParcels(any(), any());
    }

    @Test
    public void testUpgradeClusterManagerShouldAddCsdToPillarWhenTheClusterTypeIsWorkload() throws CloudbreakOrchestratorException, CloudbreakException {
        stack.setType(StackType.WORKLOAD);

        underTest.upgradeClusterManager(STACK_ID, true);

        verify(gatewayConfigService, times(1)).getGatewayConfig(stack, stack.getSecurityConfig(), stack.getPrimaryGatewayInstance(),
                cluster.getGateway() != null);
        verify(clusterComponentConfigProvider, times(1)).getClouderaManagerRepoDetails(cluster.getId());
        verify(hostOrchestrator, times(1)).upgradeClusterManager(any(), any(), any(), any(), any());
        verify(clusterApi).stopCluster(true);
        verify(clusterHostServiceRunner, times(1)).decoratePillarWithClouderaManagerRepo(any(), any(), any());
        verify(clusterHostServiceRunner, times(1)).createPillarWithClouderaManagerSettings(any(), any(), any());
        verify(clusterApi).startCluster();
        verify(csdParcelDecorator, times(1)).decoratePillarWithCsdParcels(any(), any());
    }
}
