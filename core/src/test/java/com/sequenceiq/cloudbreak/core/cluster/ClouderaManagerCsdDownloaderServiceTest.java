package com.sequenceiq.cloudbreak.core.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.CsdParcelDecorator;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerCsdDownloaderServiceTest {

    private static final long STACK_ID = 1L;

    private static final int MAX_RETRY_ON_ERROR = 3;

    private static final int MAX_RETRY = 200;

    private static final String STATE = "cloudera/csd/init";

    @InjectMocks
    private ClouderaManagerCsdDownloaderService underTest;

    @Mock
    private SaltStateParamsService saltStateParamsService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private CsdParcelDecorator csdParcelDecorator;

    @Mock
    private ClusterManagerRestartService clusterManagerRestartService;

    @Test
    void testDownloadCsdFilesShouldDownloadTheCsdFilesWithoutCmRestart() throws CloudbreakOrchestratorFailedException {
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        Set<ClouderaManagerProduct> upgradeCandidateProducts = new HashSet<>();
        OrchestratorStateParams orchestratorStateParams = mock(OrchestratorStateParams.class);
        Map<String, SaltPillarProperties> pillarProperties = mock(HashMap.class);

        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getId()).thenReturn(STACK_ID);
        when(csdParcelDecorator.addCsdParcelsToServicePillar(upgradeCandidateProducts)).thenReturn(pillarProperties);
        when(saltStateParamsService.createStateParamsForReachableNodes(stackDto, STATE, MAX_RETRY, MAX_RETRY_ON_ERROR)).thenReturn(orchestratorStateParams);

        underTest.downloadCsdFiles(stackDto, true, upgradeCandidateProducts);

        verify(clusterHostServiceRunner).redeployStates(stackDto);
        verify(hostOrchestrator).saveCustomPillars(any(), any(), any());
        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParams);
        verifyNoInteractions(clusterManagerRestartService);
    }

    @Test
    void testDownloadCsdFilesShouldDownloadTheCsdFilesWithCmRestart() throws CloudbreakOrchestratorFailedException {
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        Set<ClouderaManagerProduct> upgradeCandidateProducts = new HashSet<>();
        OrchestratorStateParams orchestratorStateParams = mock(OrchestratorStateParams.class);
        Map<String, SaltPillarProperties> pillarProperties = mock(HashMap.class);

        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getId()).thenReturn(STACK_ID);
        when(csdParcelDecorator.addCsdParcelsToServicePillar(upgradeCandidateProducts)).thenReturn(pillarProperties);
        when(saltStateParamsService.createStateParamsForReachableNodes(stackDto, STATE, MAX_RETRY, MAX_RETRY_ON_ERROR)).thenReturn(orchestratorStateParams);

        underTest.downloadCsdFiles(stackDto, false, upgradeCandidateProducts);

        verify(clusterHostServiceRunner).redeployStates(stackDto);
        verify(hostOrchestrator).saveCustomPillars(any(), any(), any());
        verify(hostOrchestrator).runOrchestratorState(orchestratorStateParams);
        verify(clusterManagerRestartService).restartClouderaManager(stackDto);
    }

    @Test
    void testDownloadCsdFilesShouldNotDownloadTheCsdFilesThenThereIsNoCandidateProduct() {
        StackDto stackDto = mock(StackDto.class);
        Set<ClouderaManagerProduct> upgradeCandidateProducts = new HashSet<>();

        when(csdParcelDecorator.addCsdParcelsToServicePillar(upgradeCandidateProducts)).thenReturn(Collections.emptyMap());

        underTest.downloadCsdFiles(stackDto, false, upgradeCandidateProducts);
    }

}