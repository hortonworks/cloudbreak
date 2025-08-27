package com.sequenceiq.cloudbreak.core.cluster;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.CmServerQueryService;

@ExtendWith(MockitoExtension.class)
public class ClusterManagerUpgradeManagementServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String OLD_CM_VERSION = "7.2.2-12345";

    private static final String CM_VERSION = "7.2.6-12345";

    private static final String CM_VERSION_WITH_P = "7.2.6-12345p";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private CmServerQueryService cmServerQueryService;

    @Mock
    private ClusterUpgradeService clusterUpgradeService;

    @Mock
    private ClusterManagerUpgradeService clusterManagerUpgradeService;

    @Mock
    private ClusterSetupService clusterSetupService;

    @Mock
    private ClouderaManagerRepo clouderaManagerRepo;

    @InjectMocks
    private ClusterManagerUpgradeManagementService underTest;

    @Spy
    private StackDto stackDto;

    private Stack stack;

    private Cluster cluster;

    private static Stream<Arguments> cmVersions() {
        return Stream.of(
                Arguments.of(CM_VERSION, CM_VERSION, false, false, 1),
                Arguments.of(CM_VERSION, CM_VERSION, true, false, 1),
                Arguments.of(CM_VERSION_WITH_P, CM_VERSION, false, true, 3),
                Arguments.of(CM_VERSION_WITH_P, CM_VERSION, true, false, 1),
                Arguments.of(CM_VERSION, CM_VERSION_WITH_P, false, true, 3),
                Arguments.of(CM_VERSION_WITH_P, CM_VERSION_WITH_P, true, false, 1)
        );
    }

    @BeforeEach
    public void setUp() {
        stack = TestUtil.stack(Status.AVAILABLE, TestUtil.awsCredential());
        cluster = TestUtil.cluster();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(cmServerQueryService.queryCmVersion(stackDto)).thenReturn(Optional.empty());
        lenient().when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        lenient().when(clusterApi.clusterSetupService()).thenReturn(clusterSetupService);
    }

    @ParameterizedTest
    @MethodSource("cmVersions")
    public void testUpgradeClusterManager(String versionOnHost, String versionInRepo, boolean rollingUpgradeEnabled, boolean stopServices,
            int expectedClusterApiCalls) throws CloudbreakOrchestratorException, CloudbreakException {
        when(stackDto.getStack()).thenReturn(stack);
        when(clouderaManagerRepo.getFullVersion()).thenReturn(versionInRepo);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(cluster.getId())).thenReturn(clouderaManagerRepo);
        when(cmServerQueryService.queryCmVersion(stackDto)).thenReturn(Optional.of(OLD_CM_VERSION)).thenReturn(Optional.of(versionOnHost));

        underTest.upgradeClusterManager(STACK_ID, rollingUpgradeEnabled);

        if (stopServices) {
            verify(clusterApiConnectors, times(expectedClusterApiCalls)).getConnector(stackDto);
            verify(clusterApi).stopCluster(true);
        }
        verify(clusterApi).startClusterManagerAndAgents();
        verify(cmServerQueryService, times(2)).queryCmVersion(stackDto);
        verify(clusterUpgradeService).upgradeClusterManager(STACK_ID);
        verify(clusterManagerUpgradeService).upgradeClouderaManager(stackDto, clouderaManagerRepo);
    }

    @Test
    public void testUpgradeClusterManagerWhenCmVersionCollectionFails() throws CloudbreakOrchestratorException, CloudbreakException {
        when(stackDto.getStack()).thenReturn(stack);
        when(clouderaManagerRepo.getFullVersion()).thenReturn(CM_VERSION);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(cluster.getId())).thenReturn(clouderaManagerRepo);
        when(cmServerQueryService.queryCmVersion(stackDto))
                .thenThrow(new CloudbreakServiceException("version mismatch error"))
                .thenReturn(Optional.of(CM_VERSION));

        underTest.upgradeClusterManager(STACK_ID, false);

        verify(clusterApiConnectors, times(3)).getConnector(stackDto);
        verify(clusterApi).stopCluster(true);
        verify(clusterApi).startClusterManagerAndAgents();
        verify(cmServerQueryService, times(2)).queryCmVersion(stackDto);
        verify(clusterUpgradeService).upgradeClusterManager(STACK_ID);
        verify(clusterManagerUpgradeService).upgradeClouderaManager(stackDto, clouderaManagerRepo);
    }

    @Test
    public void testUpgradeClusterManagerVersionIsDifferentAfterTheUpgrade() throws CloudbreakOrchestratorException, CloudbreakException {
        when(clouderaManagerRepo.getFullVersion()).thenReturn(CM_VERSION);
        when(stackDto.getStack()).thenReturn(stack);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(cluster.getId())).thenReturn(clouderaManagerRepo);
        when(cmServerQueryService.queryCmVersion(stackDto)).thenReturn(Optional.of(OLD_CM_VERSION)).thenReturn(Optional.of("wrong"));

        assertThrows(CloudbreakServiceException.class, () -> underTest.upgradeClusterManager(STACK_ID, true));

        verify(cmServerQueryService, times(2)).queryCmVersion(stackDto);
        verify(clusterUpgradeService).upgradeClusterManager(STACK_ID);
        verify(clusterManagerUpgradeService).upgradeClouderaManager(stackDto, clouderaManagerRepo);
        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    public void testUpgradeClusterManagerShouldSkipUpgradeWhenTheRequiredCmVersionIsAlreadyInstalled()
            throws CloudbreakOrchestratorException, CloudbreakException {
        when(clouderaManagerRepo.getFullVersion()).thenReturn(CM_VERSION);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(cluster.getId())).thenReturn(clouderaManagerRepo);
        when(cmServerQueryService.queryCmVersion(stackDto)).thenReturn(Optional.of(CM_VERSION));

        underTest.upgradeClusterManager(STACK_ID, true);

        verify(clusterComponentConfigProvider).getClouderaManagerRepoDetails(cluster.getId());
        verify(cmServerQueryService).queryCmVersion(stackDto);
        verify(clusterApiConnectors, times(2)).getConnector(stackDto);
        verify(clusterApi).startClusterManagerAndAgents();
        verify(clusterSetupService, atLeast(1)).updateConfig();
        verifyNoInteractions(clusterUpgradeService, clusterManagerUpgradeService);
    }
}
