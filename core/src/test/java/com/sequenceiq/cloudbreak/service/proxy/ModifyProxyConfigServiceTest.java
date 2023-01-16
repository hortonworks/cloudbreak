package com.sequenceiq.cloudbreak.service.proxy;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.service.orchestrator.OrchestratorStateParamsProvider;

@ExtendWith(MockitoExtension.class)
class ModifyProxyConfigServiceTest {

    private static final long STACK_ID = 1L;

    @Mock
    private OrchestratorStateParamsProvider orchestratorStateParamsProvider;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private ClusterBuilderService clusterBuilderService;

    @InjectMocks
    private ModifyProxyConfigService underTest;

    @Mock
    private OrchestratorStateParams stateParams;

    @BeforeEach
    void setUp() {
        lenient().when(orchestratorStateParamsProvider.createStateParams(anyLong(), anyString())).thenReturn(stateParams);
    }

    @Test
    void applyModifyProxyState() throws CloudbreakOrchestratorException {
        underTest.applyModifyProxyState(STACK_ID);

        verify(orchestratorStateParamsProvider).createStateParams(STACK_ID, ModifyProxyConfigService.MODIFY_PROXY_STATE);
        verify(hostOrchestrator).runOrchestratorState(stateParams);
    }

    @Test
    void updateClusterManager() {
        underTest.updateClusterManager(STACK_ID);

        verify(clusterBuilderService).modifyProxyConfig(STACK_ID);
    }

}
